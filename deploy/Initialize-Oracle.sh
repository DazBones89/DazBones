#!/usr/bin/env bash
# Run on the web VM. The administrator password is entered at MySQL's prompt.
set -euo pipefail
umask 077
cd "$(dirname "$0")"
if [[ ! -f .env ]]; then
  db_password="Aa9!$(openssl rand -hex 12)"
  admin_code=$(openssl rand -hex 16)
  editor_code=$(openssl rand -hex 16)
  printf '%s\n' "DB_PASSWORD=$db_password" "ADMIN_CODE=$admin_code" "EDITOR_CODE=$editor_code" > .env
fi
chmod 600 .env
# Only accept the generated format; never execute the environment file as code.
db_password=$(sed -n 's/^DB_PASSWORD=//p' .env)
# Upgrade the initial hex-only password rejected by Oracle's password policy.
# Never replace credentials once a successful setup has been recorded.
if [[ "$db_password" =~ ^[0-9a-f]{48}$ && ! -f .db-setup-complete ]]; then
  db_password="Aa9!$(openssl rand -hex 12)"
  env_file=$(mktemp .env.XXXXXX)
  sed "s/^DB_PASSWORD=.*/DB_PASSWORD=$db_password/" .env > "$env_file"
  mv "$env_file" .env
fi
[[ "$db_password" =~ ^Aa9![0-9a-f]{24}$ ]] || { echo 'Unexpected DB_PASSWORD format; stopped.'; exit 1; }
sql_file=$(mktemp)
trap 'rm -f "$sql_file"' EXIT
cat > "$sql_file" <<SQL
CREATE DATABASE IF NOT EXISTS dazbones CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'dazbones'@'10.0.0.50' IDENTIFIED BY '$db_password' REQUIRE SSL;
GRANT ALL PRIVILEGES ON dazbones.* TO 'dazbones'@'10.0.0.50';
SQL
echo 'Enter the DB administrator password at the following prompt.'
mysql --ssl-mode=REQUIRED -h 10.0.0.85 -u dazadmin -p < "$sql_file"
MYSQL_PWD="$db_password" mysql --ssl-mode=REQUIRED -h 10.0.0.85 -u dazbones -D dazbones -e 'SELECT CURRENT_USER(), VERSION();'
touch .db-setup-complete
echo 'DB_SETUP_OK'
