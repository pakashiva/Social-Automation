#!/usr/bin/env bash
# Per-boot startup for the ELVA SocialAI development environment.
# Starts PostgreSQL and ensures the database exists, then returns.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

PG_VERSION=16
DB_NAME=elva_dev
DB_USER=elva
DB_PASS=elva

echo "==> Starting PostgreSQL"
sudo pg_ctlcluster "$PG_VERSION" main start 2>/dev/null || true
for _ in $(seq 1 30); do
  sudo -u postgres psql -tc "SELECT 1" >/dev/null 2>&1 && break
  sleep 1
done

echo "==> Ensuring database role and database exist"
sudo -u postgres psql -tc "SELECT 1 FROM pg_roles WHERE rolname='${DB_USER}'" | grep -q 1 \
  || sudo -u postgres psql -c "CREATE ROLE ${DB_USER} LOGIN PASSWORD '${DB_PASS}';"
sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'" | grep -q 1 \
  || sudo -u postgres createdb -O "${DB_USER}" "${DB_NAME}"

echo "==> PostgreSQL ready"
