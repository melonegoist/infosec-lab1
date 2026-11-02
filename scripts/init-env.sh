#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ -e .env ]]; then
  echo "init-env: .env already exists, leaving it untouched" >&2
  exit 0
fi

umask 077
cat > .env <<EOF
JWT_SECRET=$(openssl rand -base64 32)
DB_APP_USERNAME=lab1_app
DB_APP_PASSWORD=$(openssl rand -hex 24)
DB_OWNER_USERNAME=lab1_owner
DB_OWNER_PASSWORD=$(openssl rand -hex 24)
APP_BOOTSTRAP_USERNAME=demo
APP_BOOTSTRAP_PASSWORD=$(openssl rand -hex 16)
EOF
echo "init-env: created .env (mode 600)"
