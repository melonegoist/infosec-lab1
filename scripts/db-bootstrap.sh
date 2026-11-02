#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

readonly REQUIRED_VARS="DB_OWNER_USERNAME DB_OWNER_PASSWORD DB_APP_USERNAME DB_APP_PASSWORD"

load_env_file() {
  local line key value
  [[ -f .env ]] || return 0
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ -z "$line" || "$line" == \#* ]] && continue
    key="${line%%=*}"
    value="${line#*=}"
    case " $REQUIRED_VARS " in
      *" $key "*) [[ -z "${!key:-}" ]] && export "$key=$value" ;;
    esac
  done < .env
}

fail() {
  echo "db-bootstrap: $*" >&2
  exit 1
}

load_env_file

for var in $REQUIRED_VARS; do
  [[ -n "${!var:-}" ]] || fail "$var is not set (see .env.example)"
done
for var in DB_OWNER_USERNAME DB_APP_USERNAME; do
  [[ "${!var}" =~ ^[a-z_][a-z0-9_]{0,62}$ ]] || fail "$var must match ^[a-z_][a-z0-9_]{0,62}$"
done
for var in DB_OWNER_PASSWORD DB_APP_PASSWORD; do
  value="${!var}"
  (( ${#value} >= 24 )) || fail "$var must be at least 24 characters (generate: openssl rand -hex 24)"
done
[[ "$DB_OWNER_USERNAME" != "$DB_APP_USERNAME" ]] || fail "owner and app roles must differ (least privilege)"

exec "${PSQL:-psql}" -X -v ON_ERROR_STOP=1 -d "${PGDATABASE:-postgres}" -f db/bootstrap/bootstrap.sql
