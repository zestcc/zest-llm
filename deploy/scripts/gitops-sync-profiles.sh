#!/usr/bin/.env bash
# GitOps：将 deploy/examples/agent-profile-*.yaml 导入 Admin
set -euo pipefail

ADMIN_URL="${ADMIN_URL:-http://localhost:8088}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
EXAMPLES_DIR="${EXAMPLES_DIR:-$ROOT/deploy/examples}"
PUBLISH="${PUBLISH:-false}"

TOKEN=$(curl -s -X POST "$ADMIN_URL/api/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
[ -n "$TOKEN" ] || { echo "FAIL: admin login"; exit 1; }

for file in "$EXAMPLES_DIR"/agent-profile-*.yaml "$EXAMPLES_DIR"/agent-profile-*.yml; do
  [ -f "$file" ] || continue
  task_code=$(basename "$file" | sed -E 's/^agent-profile-([^.]+)\.(yaml|yml)$/\1/')
  profile_json=$(cat "$file" | tr -d '\r')
  escaped=$(printf '%s' "$profile_json" | python -c 'import json,sys; print(json.dumps(sys.stdin.read()))')
  publish_flag=$([ "$PUBLISH" = "true" ] && echo true || echo false)
  body=$(printf '{"taskCode":"%s","version":"v1","publish":%s,"profileJson":%s}' "$task_code" "$publish_flag" "$escaped")
  echo "Import task=$task_code from $(basename "$file")"
  curl -sf -X POST "$ADMIN_URL/api/admin/agent-profiles/import" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$body" >/dev/null
done

echo "PASS gitops-sync-profiles"
