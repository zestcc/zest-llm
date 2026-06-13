#!/usr/bin/env bash
# SSO 浏览器联调后只读验证：查询 llm_admin_user.sso_subject
# 用法: bash deploy/scripts/sso-db-verify.sh
# 环境: MYSQL_HOST MYSQL_PORT MYSQL_USER MYSQL_PASSWORD MYSQL_DATABASE
set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
MYSQL_DATABASE="${MYSQL_DATABASE:-zest_llm}"
LIMIT="${LIMIT:-10}"

if ! command -v mysql >/dev/null 2>&1; then
  echo "SKIP: mysql client not found — run SQL from deploy/scripts/sso-browser-checklist.md manually" >&2
  exit 0
fi

if [ -z "$MYSQL_PASSWORD" ]; then
  echo "SKIP: MYSQL_PASSWORD not set — read-only verify skipped" >&2
  exit 0
fi

echo "== SSO DB verify (read-only) =="
echo "Host=$MYSQL_HOST:$MYSQL_PORT DB=$MYSQL_DATABASE LIMIT=$LIMIT"

mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -N -e "
SELECT id, username, sso_provider, sso_subject, email, updated_at
FROM llm_admin_user
WHERE sso_provider IS NOT NULL
ORDER BY updated_at DESC
LIMIT ${LIMIT};
"

count=$(mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -N -e "
SELECT COUNT(*) FROM llm_admin_user WHERE sso_provider IS NOT NULL AND sso_subject IS NOT NULL AND sso_subject <> '';
")

if [ "${count:-0}" -gt 0 ]; then
  echo "PASS sso-db-verify — $count user(s) with sso_subject"
else
  echo "WARN sso-db-verify — no sso_subject rows; complete browser SSO login first"
  exit 1
fi
