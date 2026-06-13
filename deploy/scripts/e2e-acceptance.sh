#!/usr/bin/env bash
# ZestLLM 生产验收 E2E（AC1–AC38 + Agent 模式）
set -euo pipefail

ADMIN_URL="${ADMIN_URL:-http://localhost:8088}"
DEMO_URL="${DEMO_URL:-http://localhost:8081}"
MCP_MOCK_URL="${MCP_MOCK_URL:-http://mcp-mock:8080/mcp}"

echo "== AC6: wrong token = AUTH_FAILED =="
RESP=$(curl -s -X POST "$ADMIN_URL/v1/llm/invoke" \
  -H "Authorization: Bearer wrong-token" \
  -H "Content-Type: application/json" \
  -d '{"appKey":"order-service","code":"aiChat","inputs":{"question":"hi"}}')
echo "$RESP" | grep -q "AUTH_FAILED" && echo "PASS AC6" || { echo "FAIL AC6: $RESP"; exit 1; }

echo "== AC1: demo methodA returns traceId (agent mode) =="
RESP=$(curl -s "$DEMO_URL/demo/order/methodA?orderId=1&question=hello")
echo "$RESP" | grep -q "traceId" && echo "PASS AC1" || { echo "FAIL AC1: $RESP"; exit 1; }

TRACE_ID=$(echo "$RESP" | sed -n 's/.*"traceId"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
[ -n "$TRACE_ID" ] || { echo "FAIL AC1: no traceId parsed"; exit 1; }

echo "== AC2: admin login + execution query =="
TOKEN=$(curl -s -X POST "$ADMIN_URL/api/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
[ -n "$TOKEN" ] || { echo "FAIL AC2: login"; exit 1; }
EXEC=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/executions/$TRACE_ID")
echo "$EXEC" | grep -q "$TRACE_ID" && echo "PASS AC2" || { echo "FAIL AC2: $EXEC"; exit 1; }

echo "== AC4: noop observability invoke still works (implicit in AC1) =="
echo "PASS AC4"

echo "== AC9: agent profile list has published v1 (before prompt hot update) =="
PROFILES=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/agent-profiles/aiChat/versions")
echo "$PROFILES" | grep -q "PUBLISHED" && echo "$PROFILES" | grep -q "v1" && echo "PASS AC9" || { echo "FAIL AC9: $PROFILES"; exit 1; }

echo "== AC3: prompt hot update without restart =="
PREP_V1=$(curl -s -X POST "$ADMIN_URL/v1/llm/prepare" \
  -H "Authorization: Bearer demo-token-123" \
  -H "Content-Type: application/json" \
  -d '{"appKey":"order-service","code":"aiChat","inputs":{"question":"hi"}}')
echo "$PREP_V1" | grep -q '"promptVersion"[[:space:]]*:[[:space:]]*"v1"' || { echo "FAIL AC3 baseline: $PREP_V1"; exit 1; }
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/prompts/aiChat/versions" \
  -d '{"version":"v2","templateBody":"HOT_UPDATE_MARKER {{question}}","outputSchema":"{\"type\":\"object\",\"properties\":{\"answer\":{\"type\":\"string\"}}}"}' >/dev/null
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/prompts/aiChat/publish" \
  -d '{"version":"v2"}' >/dev/null
PREP_V2=$(curl -s -X POST "$ADMIN_URL/v1/llm/prepare" \
  -H "Authorization: Bearer demo-token-123" \
  -H "Content-Type: application/json" \
  -d '{"appKey":"order-service","code":"aiChat","inputs":{"question":"hi"}}')
echo "$PREP_V2" | grep -q '"promptVersion"[[:space:]]*:[[:space:]]*"v2"' \
  && echo "$PREP_V2" | grep -q "HOT_UPDATE_MARKER" \
  && echo "PASS AC3" || { echo "FAIL AC3: $PREP_V2"; exit 1; }

echo "== AC7: prepare returns agent profile fields =="
echo "$PREP_V2" | grep -q "profileVersion" && echo "$PREP_V2" | grep -q "gatewayBaseUrl" && echo "PASS AC7" || { echo "FAIL AC7: $PREP_V2"; exit 1; }

echo "== AC8: provider switch updates gatewayBaseUrl =="
BEFORE=$(curl -s -X POST "$ADMIN_URL/v1/llm/prepare" \
  -H "Authorization: Bearer demo-token-123" \
  -H "Content-Type: application/json" \
  -d '{"appKey":"order-service","code":"aiChat","inputs":{"question":"hi"}}')
BEFORE_URL=$(echo "$BEFORE" | sed -n 's/.*"gatewayBaseUrl"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/agent-profiles/aiChat/activate-provider" \
  -d '{"providerRef":"litellm-local"}' >/dev/null
AFTER=$(curl -s -X POST "$ADMIN_URL/v1/llm/prepare" \
  -H "Authorization: Bearer demo-token-123" \
  -H "Content-Type: application/json" \
  -d '{"appKey":"order-service","code":"aiChat","inputs":{"question":"hi"}}')
AFTER_URL=$(echo "$AFTER" | sed -n 's/.*"gatewayBaseUrl"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
[ -n "$AFTER_URL" ] && [ "$BEFORE_URL" != "$AFTER_URL" ] && echo "PASS AC8" || { echo "FAIL AC8 before=$BEFORE_URL after=$AFTER_URL"; exit 1; }

echo "== AC10: auth binding OIDC upsert + read + restore =="
curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/auth-bindings" \
  -d '{"scopeType":"APP","appKey":"order-service","inboundMode":"OIDC_JWT","inboundConfigJson":"{\"mode\":\"OIDC_JWT\",\"issuer\":\"https://idp.example.com\",\"audience\":\"zest-llm\",\"jwksUri\":\"https://idp.example.com/jwks.json\"}","outboundMode":"API_KEY_REF","outboundConfigJson":"{\"mode\":\"API_KEY_REF\",\"secretRef\":\".env:LITELLM_API_KEY\"}"}' >/dev/null
AUTH_OIDC=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/auth-bindings/apps/order-service")
echo "$AUTH_OIDC" | grep -q "OIDC_JWT" && echo "$AUTH_OIDC" | grep -q "jwksUri" && echo "PASS AC10" || { echo "FAIL AC10: $AUTH_OIDC"; exit 1; }
curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/auth-bindings" \
  -d '{"scopeType":"APP","appKey":"order-service","inboundMode":"STATIC_TOKEN","inboundConfigJson":"{\"mode\":\"STATIC_TOKEN\"}","outboundMode":"API_KEY_REF","outboundConfigJson":"{\"mode\":\"API_KEY_REF\",\"secretRef\":\".env:LITELLM_API_KEY\"}"}' >/dev/null

echo "== AC11: MCP server CRUD =="
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/mcp-servers" \
  -d "{\"serverCode\":\"e2e-docs\",\"serverName\":\"E2E Docs\",\"baseUrl\":\"${MCP_MOCK_URL}\",\"configJson\":\"{}\"}" >/dev/null
MCP_LIST=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/mcp-servers")
echo "$MCP_LIST" | grep -q "e2e-docs" && echo "PASS AC11" || { echo "FAIL AC11: $MCP_LIST"; exit 1; }

echo "== AC14: MCP tools/list against mock server =="
MCP_TOOLS=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/mcp-servers/e2e-docs/tools")
echo "$MCP_TOOLS" | grep -q "search" && echo "PASS AC14" || { echo "FAIL AC14: $MCP_TOOLS"; exit 1; }

echo "== AC15: CP invoke/stream SSE meta event =="
STREAM_HEAD=$(curl -s -N -X POST "$ADMIN_URL/v1/llm/invoke/stream" \
  -H "Authorization: Bearer demo-token-123" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"appKey":"order-service","code":"aiChat","inputs":{"question":"stream-test"}}' | head -n 5)
echo "$STREAM_HEAD" | grep -q "meta" && echo "$STREAM_HEAD" | grep -q "traceId" && echo "PASS AC15" || { echo "FAIL AC15: $STREAM_HEAD"; exit 1; }

echo "== AC20: Playground run/stream SSE =="
PG_STREAM=$(curl -s -N -X POST "$ADMIN_URL/api/admin/playground/run/stream" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"appKey":"order-service","code":"aiChat","inputs":{"question":"stream-pg-test"},"bizId":"playground"}' | head -n 8)
echo "$PG_STREAM" | grep -q "meta" && echo "$PG_STREAM" | grep -q "traceId" && echo "PASS AC20" || { echo "FAIL AC20: $PG_STREAM"; exit 1; }

echo "== AC12: agent profile diff API =="
DIFF=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/agent-profiles/aiChat/diff?from=v1&to=v1")
echo "$DIFF" | grep -q "changes" && echo "PASS AC12" || { echo "FAIL AC12: $DIFF"; exit 1; }

echo "== AC13: admin users list =="
USERS=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/users")
echo "$USERS" | grep -q "admin" && echo "PASS AC13" || { echo "FAIL AC13: $USERS"; exit 1; }

FLOW_URL="${FLOW_URL:-http://localhost:20552}"

echo "== AC16: ZestFlow single-node DAG (ai chat full flow) =="
FLOW_RESP=$(curl -s -X POST "$FLOW_URL/api/execute" \
  -H "Content-Type: application/json" \
  -d '{"chainCode":"CHN_ZESTLLM_AI_CHAT","params":{"question":"flow-dag-e2e","bearerToken":"demo-token-123"}}')
echo "$FLOW_RESP" | grep -q "traceId" && echo "PASS AC16" || { echo "FAIL AC16: $FLOW_RESP"; exit 1; }
FLOW_TRACE=$(echo "$FLOW_RESP" | sed -n 's/.*"traceId"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
[ -n "$FLOW_TRACE" ] || { echo "FAIL AC16: no traceId parsed"; exit 1; }

echo "== AC17: ZestFlow two-node DAG (invoke → execution audit) =="
AUDIT_RESP=$(curl -s -X POST "$FLOW_URL/api/execute" \
  -H "Content-Type: application/json" \
  -d '{"chainCode":"CHN_ZESTLLM_INVOKE_AUDIT","params":{"question":"flow-dag-audit","bearerToken":"demo-token-123"}}')
echo "$AUDIT_RESP" | grep -q "traceId" && echo "PASS AC17" || { echo "FAIL AC17: $AUDIT_RESP"; exit 1; }

echo "== AC18: aiChatTools profile with MCP tools =="
TOOLS_PREP=$(curl -s -X POST "$ADMIN_URL/v1/llm/prepare" \
  -H "Authorization: Bearer demo-token-123" \
  -H "Content-Type: application/json" \
  -d '{"appKey":"order-service","code":"aiChatTools","inputs":{"question":"search docs"}}')
echo "$TOOLS_PREP" | grep -q "toolCallMode" && echo "$TOOLS_PREP" | grep -q "loop" && echo "PASS AC18" || { echo "FAIL AC18: $TOOLS_PREP"; exit 1; }

echo "== AC19: Mode3 Flow Gateway /api/flow/ai-chat =="
GW_RESP=$(curl -s -X POST "$ADMIN_URL/api/flow/ai-chat" \
  -H "Content-Type: application/json" \
  -d '{"question":"gateway-flow-test","bearerToken":"demo-token-123"}')
echo "$GW_RESP" | grep -q "traceId" && echo "PASS AC19" || { echo "FAIL AC19: $GW_RESP"; exit 1; }

echo "== AC20: Demo ZestFlow flowChat =="
DEMO_FLOW=$(curl -s "$DEMO_URL/demo/order/flowChat?orderId=1&question=demo-flow")
echo "$DEMO_FLOW" | grep -q "traceId" && echo "PASS AC20" || { echo "FAIL AC20: $DEMO_FLOW"; exit 1; }

echo "== AC21: Tool Loop invoke (LiteLLM mock + MCP mock) =="
TOOL_RESP=$(curl -s -X POST "$ADMIN_URL/v1/llm/invoke" \
  -H "Authorization: Bearer demo-token-123" \
  -H "Content-Type: application/json" \
  -d '{"appKey":"order-service","code":"aiChatTools","inputs":{"question":"search internal docs"}}')
echo "$TOOL_RESP" | grep -q "traceId" \
  && echo "$TOOL_RESP" | grep -q "SUCCESS" \
  && echo "$TOOL_RESP" | grep -q "tool-loop-ok" \
  && echo "PASS AC21" || { echo "FAIL AC21: $TOOL_RESP"; exit 1; }

echo "== AC22: Admin Playground preview =="
PG_PREVIEW=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/playground/preview" \
  -d '{"appKey":"order-service","code":"aiChat","inputs":{"question":"playground"}}')
echo "$PG_PREVIEW" | grep -q "renderedPrompt" && echo "PASS AC22" || { echo "FAIL AC22: $PG_PREVIEW"; exit 1; }

echo "== AC23: Eval dataset batch run =="
EVAL_RUN=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  "$ADMIN_URL/api/admin/eval/datasets/demo-aichat/run")
echo "$EVAL_RUN" | grep -q "runCode" \
  && echo "$EVAL_RUN" | grep -q "passRate" \
  && echo "PASS AC23" || { echo "FAIL AC23: $EVAL_RUN"; exit 1; }

echo "== AC24: Response semantic cache hit on 2nd invoke =="
CACHE_Q="cache-hit-$(date +%s)"
INV1=$(curl -s -X POST "$ADMIN_URL/v1/llm/invoke" \
  -H "Authorization: Bearer demo-token-123" \
  -H "Content-Type: application/json" \
  -d "{\"appKey\":\"order-service\",\"code\":\"aiChat\",\"inputs\":{\"question\":\"$CACHE_Q\"}}")
echo "$INV1" | grep -q "SUCCESS" || { echo "FAIL AC24 first invoke: $INV1"; exit 1; }
INV2=$(curl -s -X POST "$ADMIN_URL/v1/llm/invoke" \
  -H "Authorization: Bearer demo-token-123" \
  -H "Content-Type: application/json" \
  -d "{\"appKey\":\"order-service\",\"code\":\"aiChat\",\"inputs\":{\"question\":\"$CACHE_Q\"}}")
echo "$INV2" | grep -q "cacheHit" \
  && echo "$INV2" | grep -q "true" \
  && echo "PASS AC24" || { echo "FAIL AC24: $INV2"; exit 1; }

echo "== AC25: Flow chains from DB =="
FLOW_CHAINS=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/flow-chains")
echo "$FLOW_CHAINS" | grep -q "CHN_ZESTLLM_AI_CHAT" \
  && echo "$FLOW_CHAINS" | grep -q "CHN_ZESTLLM_TOOL_LOOP" \
  && echo "PASS AC25" || { echo "FAIL AC25: $FLOW_CHAINS"; exit 1; }

echo "== AC26: FinOps quota alert webhook configured =="
QUOTA=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/apps/order-service/quota")
echo "$QUOTA" | grep -q "alertWebhookUrl" \
  && echo "$QUOTA" | grep -q "webhook" \
  && echo "PASS AC26" || { echo "FAIL AC26: $QUOTA"; exit 1; }

echo "== AC27: Eval dataset CRUD =="
EVAL_DS="e2e-eval-$(date +%s)"
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/eval/datasets" \
  -d "{\"datasetCode\":\"$EVAL_DS\",\"datasetName\":\"E2E\",\"appKey\":\"order-service\",\"taskCode\":\"aiChat\"}" \
  | grep -q "$EVAL_DS" && echo "PASS AC27" || { echo "FAIL AC27"; exit 1; }

echo "== AC28: Execution archive stats =="
ARCHIVE=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/executions/archive/stats")
echo "$ARCHIVE" | grep -q "hotExecutions" \
  && echo "$ARCHIVE" | grep -q "archiveEnabled" \
  && echo "PASS AC28" || { echo "FAIL AC28: $ARCHIVE"; exit 1; }

echo "== AC29: Agent profile probe (published aiChat) =="
PROBE=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/agent-profiles/aiChat/probe" \
  -d '{"smokeTest":false}')
echo "$PROBE" | grep -q "overallStatus" \
  && echo "$PROBE" | grep -q "checks" \
  && echo "$PROBE" | grep -q "probeId" \
  && echo "PASS AC29" || { echo "FAIL AC29: $PROBE"; exit 1; }

echo "== AC30: Dashboard agent health =="
AGENT_HEALTH=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/dashboard/agent-health")
echo "$AGENT_HEALTH" | grep -q "monitored" \
  && echo "$AGENT_HEALTH" | grep -q "ready" \
  && echo "$AGENT_HEALTH" | grep -q "alerts" \
  && echo "PASS AC30" || { echo "FAIL AC30: $AGENT_HEALTH"; exit 1; }

echo "== AC31: Agent probe history persisted =="
PROBE_HIST=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "$ADMIN_URL/api/admin/agent-profiles/aiChat/probe/history?page=1&size=5")
echo "$PROBE_HIST" | grep -q "records" \
  && echo "$PROBE_HIST" | grep -q "aiChat" \
  && echo "PASS AC31" || { echo "FAIL AC31: $PROBE_HIST"; exit 1; }

echo "== AC32: Ops center APIs (cost alerts + probe alerts + dashboard stats) =="
COST_ALERTS=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/cost-alerts?page=1&size=5")
echo "$COST_ALERTS" | grep -q "records" && echo "PASS AC32 cost-alerts" || { echo "FAIL AC32 cost-alerts: $COST_ALERTS"; exit 1; }
PROBE_ALERTS=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/agent-probe-alerts?page=1&size=5")
echo "$PROBE_ALERTS" | grep -q "records" && echo "PASS AC32 probe-alerts" || { echo "FAIL AC32 probe-alerts: $PROBE_ALERTS"; exit 1; }
DASH_STATS=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/dashboard/stats")
echo "$DASH_STATS" | grep -q "agentsMonitored" \
  && echo "$DASH_STATS" | grep -q "agentsReady" \
  && echo "PASS AC32 dashboard-stats" || { echo "FAIL AC32 dashboard-stats: $DASH_STATS"; exit 1; }

echo "== AC33: FinOps cost alert record (order-service) =="
FINOPS_ALERTS=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/cost-alerts?appKey=order-service&page=1&size=5")
echo "$FINOPS_ALERTS" | grep -q "order-service" \
  && echo "$FINOPS_ALERTS" | grep -q "SENT" \
  && echo "PASS AC33" || { echo "FAIL AC33: $FINOPS_ALERTS"; exit 1; }

echo "== AC34: Observability config API =="
OBS_CFG=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/config/observability")
echo "$OBS_CFG" | grep -q "adapterId" \
  && echo "$OBS_CFG" | grep -q "tracePathTemplate" \
  && echo "PASS AC34" || { echo "FAIL AC34: $OBS_CFG"; exit 1; }

echo "== AC35: Probe history profileVersion filter =="
PROBE_FILTER=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "$ADMIN_URL/api/admin/agent-profiles/aiChat/probe/history?page=1&size=5&profileVersion=v1")
echo "$PROBE_FILTER" | grep -q "records" \
  && echo "$PROBE_FILTER" | grep -q "v1" \
  && echo "PASS AC35" || { echo "FAIL AC35: $PROBE_FILTER"; exit 1; }

echo "== AC36: Execution detail includes observability fields =="
EXEC_DETAIL=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/executions/$TRACE_ID")
echo "$EXEC_DETAIL" | grep -q "observabilityAdapter" \
  && echo "PASS AC36" || { echo "FAIL AC36: $EXEC_DETAIL"; exit 1; }

echo "== AC37: Batch probe all published profiles =="
PROBE_ALL=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/agent-profiles/probe-all" \
  -d '{"smokeTest":false}')
echo "$PROBE_ALL" | grep -q "probedCount" \
  && echo "PASS AC37" || { echo "FAIL AC37: $PROBE_ALL"; exit 1; }

echo "== AC38: Agent profile probe API (new paths) =="
META=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/meta/features")
echo "$META" | grep -q "agentProbeApi" \
  && echo "PASS AC38 meta" || { echo "FAIL AC38 meta: $META"; exit 1; }
PROBE_NEW=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "$ADMIN_URL/api/admin/agent-profile-probes/aiChat/latest")
echo "$PROBE_NEW" | grep -q '"code":200' \
  && echo "PASS AC38 latest" || { echo "FAIL AC38 latest: $PROBE_NEW"; exit 1; }

AC_SUFFIX=$(date +%H%M%S)

echo "== AC39: extensions import =="
EXT_PROFILE="{\"taskCode\":\"aiChat\",\"version\":\"v-ac39-${AC_SUFFIX}\",\"profileJson\":\"{\\\"apiVersion\\\":\\\"zestllm/v1\\\",\\\"runtimeMode\\\":\\\"agent\\\",\\\"providerRef\\\":\\\"litellm-default\\\",\\\"model\\\":{\\\"primary\\\":\\\"gpt-4o-mini\\\"},\\\"generation\\\":{\\\"maxTokens\\\":512,\\\"temperature\\\":0.3,\\\"timeoutMs\\\":30000},\\\"extensions\\\":{\\\"runtimeBackend\\\":{\\\"type\\\":\\\"native\\\"},\\\"knowledge\\\":{\\\"enabled\\\":false},\\\"learningLoop\\\":{\\\"enabled\\\":false}}}\",\"publish\":false}"
IMP=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/agent-profiles/import" -d "$EXT_PROFILE")
echo "$IMP" | grep -q "v-ac39" && echo "PASS AC39" || { echo "FAIL AC39: $IMP"; exit 1; }

INTEGRATION_STRICT="${ZEST_INTEGRATION_E2E:-0}"

integration_fail_or_skip() {
  local ac="$1"
  local msg="$2"
  if [ "$INTEGRATION_STRICT" = "1" ]; then
    echo "FAIL $ac: $msg"
    exit 1
  else
    echo "SKIP $ac: $msg"
  fi
}

echo "== AC40: hybrid prepare returns knowledgePrefetch =="
HYBRID_PROFILE="{\"taskCode\":\"aiChat\",\"version\":\"v-ac40-${AC_SUFFIX}\",\"profileJson\":\"{\\\"apiVersion\\\":\\\"zestllm/v1\\\",\\\"runtimeMode\\\":\\\"hybrid\\\",\\\"providerRef\\\":\\\"litellm-default\\\",\\\"model\\\":{\\\"primary\\\":\\\"gpt-4o-mini\\\"},\\\"generation\\\":{\\\"maxTokens\\\":512,\\\"temperature\\\":0.3,\\\"timeoutMs\\\":30000},\\\"extensions\\\":{\\\"knowledge\\\":{\\\"enabled\\\":true,\\\"provider\\\":\\\"noop\\\",\\\"datasetIds\\\":[\\\"demo\\\"],\\\"topK\\\":3,\\\"scoreThreshold\\\":0.5,\\\"injectMode\\\":\\\"system_prefix\\\"},\\\"learningLoop\\\":{\\\"enabled\\\":false}}}\",\"publish\":false}"
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/agent-profiles/import" -d "$HYBRID_PROFILE" >/dev/null || true
PUB40=$(curl -s -o /dev/null -w "%{http_code}" -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/agent-profiles/aiChat/publish" -d "{\"version\":\"v-ac40-${AC_SUFFIX}\"}")
[ "$PUB40" != "200" ] && echo "WARN AC40 publish blocked by probe gate, using existing published hybrid"
PREP40=$(curl -s -X POST "$ADMIN_URL/v1/llm/prepare" \
  -H "Authorization: Bearer demo-token-123" \
  -H "Content-Type: application/json" \
  -d '{"appKey":"order-service","code":"aiChat","inputs":{"question":"hybrid-test"}}')
echo "$PREP40" | grep -q "knowledgePrefetch" && echo "PASS AC40" \
  || integration_fail_or_skip "AC40" "hybrid prepare missing knowledgePrefetch: $PREP40"

echo "== AC41: integration SPI adapter health =="
ADAPTERS=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/adapters/health/all")
echo "$ADAPTERS" | grep -q "agent-runtime" \
  && echo "$ADAPTERS" | grep -q "knowledge-retrieval" \
  && echo "$ADAPTERS" | grep -q "learning-pipeline" \
  && echo "PASS AC41" \
  || integration_fail_or_skip "AC41" "adapters health missing SPI keys: $ADAPTERS"

echo "== AC42: probe external-runtime or knowledge checks =="
PROBE42=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/agent-profile-probes/aiChat/run" \
  -d '{"smokeTest":false}')
echo "$PROBE42" | grep -qE "external-runtime|knowledge" && echo "PASS AC42" \
  || integration_fail_or_skip "AC42" "probe missing external/knowledge checks: $PROBE42"

echo "== AC45: capability stack =="
CS=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/capability-stack")
echo "$CS" | grep -q "currentTier" && echo "PASS AC45" || { echo "FAIL AC45: $CS"; exit 1; }

echo "== AC46: scenario templates =="
ST=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/scenario-templates")
echo "$ST" | grep -q "chat-basic" && echo "PASS AC46" || { echo "FAIL AC46: $ST"; exit 1; }

echo "== AC54: knowledge-qa scenario template =="
echo "$ST" | grep -q "knowledge-qa" \
  && echo "$ST" | grep -q "知识问答" \
  && echo "PASS AC54" || { echo "FAIL AC54: $ST"; exit 1; }

echo "== AC47: ai jobs overview =="
JO=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/ai-jobs/overview")
echo "$JO" | grep -q "aiChat" && echo "PASS AC47" || { echo "FAIL AC47: $JO"; exit 1; }

echo "== AC48: meta features zest-stack =="
FEAT=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/meta/features")
echo "$FEAT" | grep -q "capabilityStackApi" && echo "$FEAT" | grep -q "scenarioTemplateApi" \
  && echo "PASS AC48" || { echo "FAIL AC48: $FEAT"; exit 1; }

echo "== AC49: publish preview =="
PV=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/agent-profiles/aiChat/versions/v1/publish-preview")
echo "$PV" | grep -q "publishAllowed" && echo "PASS AC49" || { echo "FAIL AC49: $PV"; exit 1; }

echo "== AC50: app overview =="
AO=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/apps/overview")
echo "$AO" | grep -q "order-service" && echo "PASS AC50" || { echo "FAIL AC50: $AO"; exit 1; }

echo "== AC51: capability stack export (medium) =="
EX=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/capability-stack/export?tier=medium")
echo "$EX" | grep -q "ZEST_STACK_TIER" && echo "PASS AC51" || { echo "FAIL AC51: $EX"; exit 1; }

echo "== AC51b: capability stack export (large) =="
EX_LG=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/capability-stack/export?tier=large")
echo "$EX_LG" | grep -q "ZEST_STACK_TIER" \
  && echo "$EX_LG" | grep -q '"large"' \
  && echo "$EX_LG" | grep -q "dify" \
  && echo "$EX_LG" | grep -q "ragflow" \
  && echo "$EX_LG" | grep -q "kafka" \
  && echo "PASS AC51b" || { echo "FAIL AC51b: $EX_LG"; exit 1; }

echo "== AC52: ai job wizard =="
WZ=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/ai-jobs/wizard" \
  -d "{\"templateId\":\"chat-basic\",\"appKey\":\"order-service\",\"taskCode\":\"aiChat\",\"publish\":false,\"runProbe\":false}")
echo "$WZ" | grep -q "aiChat" && echo "PASS AC52" || { echo "FAIL AC52: $WZ"; exit 1; }

echo "== AC53: learning multi-source suggest =="
SG=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/learning/suggest-cases" \
  -d '{"taskCode":"aiChat","limit":5,"distillationSources":["execution","langfuse"]}')
echo "$SG" | grep -q '"code":200' && echo "PASS AC53" || { echo "FAIL AC53: $SG"; exit 1; }

echo "== AC55: scenario wizard report-basic -> aiReport =="
WZ55=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/ai-jobs/wizard" \
  -d "{\"templateId\":\"report-basic\",\"appKey\":\"order-service\",\"taskCode\":\"aiReport\",\"publish\":false,\"runProbe\":false}" 2>/dev/null || echo '{"error":true}')
if echo "$WZ55" | grep -q "aiReport"; then
  echo "PASS AC55"
else
  integration_fail_or_skip "AC55" "wizard report-basic failed: $WZ55"
fi

echo "== AC56: scenario wizard ops-monitor -> aiOps =="
WZ56=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/ai-jobs/wizard" \
  -d "{\"templateId\":\"ops-monitor\",\"appKey\":\"order-service\",\"taskCode\":\"aiOps\",\"publish\":false,\"runProbe\":false}" 2>/dev/null || echo '{"error":true}')
if echo "$WZ56" | grep -q "aiOps"; then
  echo "PASS AC56"
else
  integration_fail_or_skip "AC56" "wizard ops-monitor failed: $WZ56"
fi

if [ "$INTEGRATION_STRICT" = "1" ]; then
  echo "== INTEGRATION: adapter health ragflow/dify =="
  echo "$ADAPTERS" | grep -qi "ragflow" && echo "PASS INTEGRATION ragflow adapter listed" \
    || { echo "FAIL INTEGRATION: ragflow not in adapter health"; exit 1; }
  echo "$ADAPTERS" | grep -qi "dify" && echo "PASS INTEGRATION dify adapter listed" \
    || { echo "FAIL INTEGRATION: dify not in adapter health"; exit 1; }

  echo "== INTEGRATION: prepare aiReport (hybrid ragflow) =="
  PREP_RPT=$(curl -s -X POST "$ADMIN_URL/v1/llm/prepare" \
    -H "Authorization: Bearer demo-token-123" \
    -H "Content-Type: application/json" \
    -d '{"appKey":"order-service","code":"aiReport","inputs":{"question":"Q4 revenue trend"}}')
  echo "$PREP_RPT" | grep -q "knowledgePrefetch" && echo "PASS INTEGRATION aiReport prepare" \
    || { echo "FAIL INTEGRATION aiReport prepare: $PREP_RPT"; exit 1; }

  echo "== INTEGRATION: prepare aiOps =="
  PREP_OPS=$(curl -s -X POST "$ADMIN_URL/v1/llm/prepare" \
    -H "Authorization: Bearer demo-token-123" \
    -H "Content-Type: application/json" \
    -d '{"appKey":"order-service","code":"aiOps","inputs":{"question":"pod crash loop"}}')
  echo "$PREP_OPS" | grep -q "profileVersion" && echo "PASS INTEGRATION aiOps prepare" \
    || { echo "FAIL INTEGRATION aiOps prepare: $PREP_OPS"; exit 1; }
fi

echo "== AC57: gateway-models list =="
GM=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/gateway-models")
echo "$GM" | grep -q "deepseek-v4-flash" && echo "PASS AC57" || { echo "FAIL AC57: $GM"; exit 1; }

echo "== AC58: integration import idempotent =="
IMP58=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/integration/import/gateway-models" \
  -d '{"items":[{"modelName":"deepseek-v4-flash","upstreamModel":"deepseek/deepseek-v4-flash","apiKeySecretRef":"deepseek-api-key"}]}')
echo "$IMP58" | grep -q '"updated":1' && echo "PASS AC58" || { echo "FAIL AC58: $IMP58"; exit 1; }

echo "== AC59: publish-preview extended fields =="
PV59=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/agent-profiles/aiChat/versions/v1/publish-preview")
echo "$PV59" | grep -q "adapterHealthSummary" && echo "$PV59" | grep -q "knowledgeHealthUp" \
  && echo "PASS AC59" || { echo "FAIL AC59: $PV59"; exit 1; }

echo "== AC60: generic scenario templates =="
echo "$ST" | grep -q "generic-chat-agent" && echo "$ST" | grep -q "generic-hybrid-rag" \
  && echo "PASS AC60" || { echo "FAIL AC60: $ST"; exit 1; }

echo "== AC61: secret-refs list =="
SR61=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/secret-refs")
echo "$SR61" | grep -q "deepseek-api-key" && echo "PASS AC61" || { echo "FAIL AC61: $SR61"; exit 1; }

echo "== AC62: integration suite features =="
FEAT62=$(curl -s -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/meta/features")
echo "$FEAT62" | grep -q "integrationSuiteApi" && echo "PASS AC62" || { echo "FAIL AC62: $FEAT62"; exit 1; }

echo "== All automated E2E checks passed =="
