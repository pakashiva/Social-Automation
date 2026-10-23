#!/usr/bin/env bash
# Loads sql/schema.sql, then sql/seed.sql when SM_DEV_SEED_PASSWORD_HASH is a BCrypt strength-10 hash.
set -euo pipefail

cd "$(dirname "$0")"

if [[ -z "${SM_DB_URL:-}" || -z "${SM_DB_USER:-}" || -z "${SM_DB_PASSWORD:-}" ]]; then
  echo "Set SM_DB_URL, SM_DB_USER, and SM_DB_PASSWORD." >&2
  exit 1
fi

without_scheme="${SM_DB_URL#jdbc:mysql://}"
hostport="${without_scheme%%/*}"
path_and_query="${without_scheme#*/}"
database="${path_and_query%%\?*}"
host="${hostport%%:*}"
port="${hostport##*:}"
if [[ "$port" == "$host" || -z "$port" ]]; then
  port=3306
fi

export MYSQL_PWD="$SM_DB_PASSWORD"

run_sql() {
  mysql --protocol=TCP -h "$host" -P "$port" -u "$SM_DB_USER" "$database"
}

{
  echo "SET NAMES utf8mb4;"
  echo "SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION,ERROR_FOR_DIVISION_BY_ZERO';"
  awk '
    /^CREATE DATABASE/ { skip = 1 }
    /^USE[[:space:]]/ { next }
    skip {
      if ($0 ~ /;[[:space:]]*$/) { skip = 0 }
      next
    }
    { print }
  ' schema.sql
} | run_sql

hash="${SM_DEV_SEED_PASSWORD_HASH:-}"
if [[ ! "$hash" =~ ^\$2a\$10\$[./A-Za-z0-9]{53}$ ]]; then
  echo "Set SM_DEV_SEED_PASSWORD_HASH to a BCrypt strength 10 hash of the dev password."
  exit 0
fi

python3 -c 'import os, pathlib, sys
sql = pathlib.Path("seed.sql").read_text()
token = "__SEED_PASSWORD_HASH__"
replacement = os.environ["SM_DEV_SEED_PASSWORD_HASH"]
if token not in sql:
    sys.exit("seed.sql is missing the password hash token")
sys.stdout.write(sql.replace(token, replacement))
' | run_sql
