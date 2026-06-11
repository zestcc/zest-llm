#!/usr/bin/.env bash
# 本地完整版验证：mvn test + Admin API 验收（Linux / macOS）
set -euo pipefail

ADMIN_URL="${ADMIN_URL:-http://127.0.0.1:8088}"
SKIP_MAVEN=false
SKIP_ACCEPTANCE=false

for arg in "$@"; do
  case "$arg" in
    --skip-maven) SKIP_MAVEN=true ;;
    --skip-acceptance) SKIP_ACCEPTANCE=true ;;
    *) echo "Unknown option: $arg" >&2; exit 1 ;;
  esac
done

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

if ! $SKIP_MAVEN; then
  echo "== mvn test =="
  (cd "$ROOT" && mvn -B test)
  echo "mvn test: OK"
fi

if ! $SKIP_ACCEPTANCE; then
  echo "== full-acceptance.ps1 (via pwsh if available, else e2e subset) =="
  SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
  if command -v pwsh >/dev/null 2>&1; then
    pwsh -NoProfile -File "$SCRIPT_DIR/full-acceptance.ps1" -AdminUrl "$ADMIN_URL"
  elif command -v powershell >/dev/null 2>&1; then
    powershell -NoProfile -File "$SCRIPT_DIR/full-acceptance.ps1" -AdminUrl "$ADMIN_URL"
  else
    echo "PowerShell not found — running e2e-acceptance.sh against $ADMIN_URL"
    ADMIN_URL="$ADMIN_URL" bash "$SCRIPT_DIR/e2e-acceptance.sh"
  fi
fi

echo "All local verification passed."
