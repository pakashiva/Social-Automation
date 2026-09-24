#!/usr/bin/env bash
# Idempotent bootstrap for the ELVA SocialAI development environment.
# Runs after the repository is checked out. Safe to run repeatedly.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

PG_VERSION=16
DB_NAME=elva_dev
DB_USER=elva
DB_PASS=elva

echo "==> Installing system packages (postgresql, build tools, python venv)"
if ! command -v pg_ctlcluster >/dev/null 2>&1; then
  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
    postgresql postgresql-contrib python3-venv build-essential
fi

echo "==> Creating Python virtual environment and installing dependencies"
if [ ! -d .venv ]; then
  python3 -m venv .venv
fi
# shellcheck disable=SC1091
source .venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt

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

echo "==> Writing local .env with development defaults (kept if it already exists)"
# Real secrets injected by Cloud Agents as environment variables take precedence,
# because python-dotenv does not override already-set environment variables.
if [ ! -f .env ]; then
  cat > .env <<EOF
# Auto-generated development defaults. Real values injected as Cloud Agent
# secrets (environment variables) override these entries.
DATABASE_URL=postgresql+psycopg://${DB_USER}:${DB_PASS}@localhost:5432/${DB_NAME}
SECRET_KEY=$(python3 -c 'import secrets; print(secrets.token_hex(32))')
JWT_SECRET_KEY=$(python3 -c 'import secrets; print(secrets.token_hex(32))')
PORT=5000

# The app instantiates the Gemini model at import time, so this must be
# non-empty for the app to boot. Set a real key to enable AI/RAG features.
GEMINI_API_KEY=placeholder-set-a-real-key-to-enable-ai-features

# External services (optional; required only for their respective features):
# CHROMA_API_KEY, TENANT_ID, CHROMA_DATABASE (RAG vector store)
# CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET
# LINKEDIN_CLIENT_ID, LINKEDIN_CLIENT_SECRET, LINKEDIN_REDIRECT_URI
# META_APP_ID, META_APP_SECRET, META_REDIRECT_URI
# NOTIFY_API_KEY, NOTIFY_APP_ID, NOTIFY_BRAND_ID, NOTIFY_EMAIL_ENDPOINT
EOF
fi

echo "==> Initializing database schema and stamping migration head"
# The initial Alembic migration is intentionally empty; the schema is created
# from the current models via db.create_all(), then migrations are stamped so
# Alembic considers the database up to date.
export FLASK_APP=main
python -c "from initialize_database.init_db import initialize_database; initialize_database()"
flask db stamp head

echo "==> Install complete"
