#!/usr/bin/env bash
set -euo pipefail
umask 077
cd "$(dirname "$0")"
root=$(pwd)
exec 9>"$root/.backup.lock"
flock -n 9 || { echo 'Another backup is running'; exit 1; }
destination=$(mktemp -d "$root/backups-$(date -u +%Y%m%dT%H%M%SZ)-XXXXXX")
password=$(sed -n 's/^DB_PASSWORD=//p' .env)
[[ -n "$password" ]] || { echo 'Missing DB credential'; exit 1; }
compose=(sudo docker compose -f compose.oracle.yml -f compose.oracle-https.yml)
running=$("${compose[@]}" ps --status running -q app)
resume() {
  if [[ -n "$running" ]]; then
    "${compose[@]}" start app
    for attempt in $(seq 1 36); do
      if curl --fail --silent --max-time 3 http://127.0.0.1:8080/health/readiness >/dev/null; then
        echo APP_READY
        return 0
      fi
      sleep 5
    done
    echo 'App readiness failed after backup' >&2
    return 1
  fi
}
trap resume EXIT
if [[ -n "$running" ]]; then "${compose[@]}" stop app; fi
MYSQL_PWD="$password" mysqldump --ssl-mode=REQUIRED -h 10.0.0.85 -u dazbones \
  --single-transaction --no-tablespaces --set-gtid-purged=OFF --hex-blob \
  --column-statistics=0 dazbones > "$destination/database.sql"
"${compose[@]}" run --rm --no-deps --user 0:0 --entrypoint tar \
  -v "$destination:/backup" app -czf /backup/images.tar.gz -C /data/images .
sudo chown "$(id -u):$(id -g)" "$destination/images.tar.gz"
chmod 600 "$destination"/*
test -s "$destination/database.sql"
gzip -t "$destination/images.tar.gz"
(cd "$destination" && sha256sum database.sql images.tar.gz > SHA256SUMS)
touch "$destination/COMPLETE"
# Prune only completed tool-created directories, after the new backup succeeded.
mapfile -t completed < <(find "$root" -maxdepth 1 -type d -name 'backups-*' | sort -r)
kept=0
for candidate in "${completed[@]}"; do
  [[ "$(basename "$candidate")" =~ ^backups-[0-9]{8}T[0-9]{6}Z-[A-Za-z0-9]{6}$ ]] || continue
  [[ ! -L "$candidate" && -f "$candidate/COMPLETE" ]] || continue
  kept=$((kept + 1))
  if (( kept > 7 )); then rm -rf -- "$candidate"; fi
done
echo "BACKUP_OK $destination"
