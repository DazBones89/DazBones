# Oracle AMD + managed MySQL deployment

The initial verification deployment uses `deploy/compose.oracle.yml` and binds
the application to `127.0.0.1:8080` on the VM. It is not the public HTTPS endpoint.
The existing all-in-one `deploy/compose.yml` is not used on this 1 GiB VM.

## Initial database setup

Copy `deploy/Initialize-Oracle.sh` and `deploy/compose.oracle.yml` to
`~/dazbones/` on the web VM. Run `bash ~/dazbones/Initialize-Oracle.sh` there,
entering the MySQL administrator password at the interactive prompt.
Do not send the administrator password through chat or command arguments.

The script creates the `dazbones` schema and an application account restricted
to source `10.0.0.50`, with privileges on that schema only and SSL required.
Generated application credentials are stored in `.env` with mode 600.
The administrator password is not saved. `DB_SETUP_OK` confirms the application
account can connect. Subsequent runs preserve generated credentials.

The original hex-only generated password was rejected by the managed database.
The script upgrades that format only before a successful setup marker exists.
New passwords contain uppercase, lowercase, numeric and special characters.

## Initial application verification

Build/export the amd64 image on the development machine, not the small VM.
Load the `dazbones-site:local` image into Docker on the VM, then run:

```sh
cd ~/dazbones
sudo docker compose -f compose.oracle.yml up -d
curl --fail http://127.0.0.1:8080/health/readiness
```

The application has a 640 MiB memory limit and a 256 MiB Java heap. Images use a
persistent Docker volume. Secure session cookies remain enabled.

## Required before public launch

- Configure a public hostname and HTTPS reverse proxy.
- Limit public inbound access to the required web ports and review SSH access.
- TLS `REQUIRED` currently encrypts DB traffic but does not validate the server
  certificate. Configure and verify CA-based validation before launch.
- Migrate retained content deliberately; the new database starts empty.
- Verify login and save operations, image persistence, and backup/restore using
  the external managed DB (existing all-in-one DB container scripts do not apply).
- Keep generated login codes private and provide them to the owner securely.

## Verified on 2026-10-06

- HTTPS is live at `https://dazbones.161-33-178-229.sslip.io` with a Let's
  Encrypt certificate and HTTP-to-HTTPS redirect. Ports 80/443 are allowed.
- Group photo and Instagram post `DVETcRUkxMO` were migrated. No roster,
  schedules or announcements were present in the cleaned source DB.
- Both login roles passed the public HTTPS access checks. Players received
  403 for master-only player settings; anonymous input API access returned 401.
- A uniquely named temporary player passed stats, fee, gear and attendance
  writes and read-back checks. Those temporary business records were removed;
  audit history was preserved.
- `Backup-Oracle.sh` captured the external DB and image volume while the app
  was stopped. It resumes the app on failure or success. Restart takes around
  1–2 minutes on this VM and existing sessions are lost.
- Backup SQL was restored into a separate local MySQL schema: six successful
  migrations, two login credentials and one Instagram post were retained.
  The restored group photo SHA256 matched the source.
- SSH password and keyboard-interactive authentication are disabled. The app
  port binds only to loopback; `.env` permissions are 600. SSH source filtering
  and DB certificate validation remain separate hardening items.
- Login codes were delivered in a Git-ignored local file containing only the
  two site codes. The DB connection password remains on the server.

The owner approved daily backup at 04:00 Asia/Tokyo with the last seven completed
backups retained. `dazbones-backup.timer` is enabled on the VM. Check failures
with `systemctl status dazbones-backup.service` and `journalctl -u dazbones-backup.service`.
The timer is persistent: a missed run executes when the VM starts again.
Manual backup command: `bash ~/dazbones/Backup-Oracle.sh`.
Download completed backup directories to a separate device; do not rely on
copies on the same VM as the only recovery source. Backups contain login hashes
and future member data and must be kept private.
