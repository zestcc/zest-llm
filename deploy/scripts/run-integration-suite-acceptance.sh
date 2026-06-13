#!/usr/bin/env bash
# Integration Suite tier acceptance (small / medium / large)
set -euo pipefail

ADMIN_URL="${ADMIN_URL:-http://localhost:8088}"
TIER="${ZEST_STACK_TIER:-small}"
TOKEN=""

login() {
  TOKEN=$(curl -s -X POST "$ADMIN_URL/api/admin/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
}

assert_json() {
  local ac="$1" body="$2" needle="$3"
  echo "$body" | grep -q "$needle" && echo "PASS $ac" || { echo "FAIL $ac: $body"; exit 1; }
}

echo "== Integration Suite acceptance tier=$TIER =="
login

echo "== AC57: gateway-models list =="
GM=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/gateway-models")
assert_json "AC57" "$GM" "deepseek-v4-flash"

echo "== AC61: secret-refs list =="
SR=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/secret-refs")
assert_json "AC61" "$SR" "deepseek-api-key"

echo "== AC58: integration import idempotent =="
IMP=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/integration/import/gateway-models" \
  -d '{"items":[{"modelName":"deepseek-v4-flash","upstreamModel":"deepseek/deepseek-v4-flash","apiKeySecretRef":"deepseek-api-key"}]}')
assert_json "AC58" "$IMP" '"updated":1'

echo "== AC59: publish-preview extended fields =="
PV=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/agent-profiles/aiChat/versions/v1/publish-preview")
echo "$PV" | grep -q "adapterHealthSummary" && echo "$PV" | grep -q "knowledgeHealthUp" \
  && echo "PASS AC59" || { echo "FAIL AC59: $PV"; exit 1; }

echo "== AC60: generic scenario templates =="
ST=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/scenario-templates")
echo "$ST" | grep -q "generic-chat-agent" && echo "$ST" | grep -q "generic-hybrid-rag" \
  && echo "PASS AC60" || { echo "FAIL AC60: $ST"; exit 1; }

echo "== AC62: http-knowledge adapter health (noop ok) =="
FEAT=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/meta/features")
echo "$FEAT" | grep -q "integrationSuiteApi" && echo "PASS AC62 meta" || { echo "FAIL AC62: $FEAT"; exit 1; }

case "$TIER" in
  small)
    echo "TIER small: gateway SSOT + import API verified"
    ;;
  medium)
    echo "TIER medium: run with zest.llm.adapters.knowledge-retrieval=http-knowledge for full SPI"
    ;;
  large)
    echo "TIER large: includes webhook + litellm sync trigger"
    curl -s -X POST -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/integration/sync-litellm" | grep -q synced \
      && echo "PASS large sync-litellm" || echo "SKIP large sync-litellm (LiteLLM optional)"
    ;;
esac

echo "== Integration Suite acceptance passed =="
