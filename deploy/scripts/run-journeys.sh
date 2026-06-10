#!/usr/bin/env bash
# 执行 .zestflow/acceptance/journeys.yml 中定义的 Admin API 旅程
set -euo pipefail

ADMIN_URL="${ADMIN_URL:-http://localhost:8088}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

JOURNEY_TOKEN=""

journey_admin_login() {
  local resp
  resp=$(curl -s -X POST "$ADMIN_URL/api/admin/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}')
  echo "$resp" | grep -q "token" || { echo "FAIL journey admin_login: $resp"; return 1; }
  JOURNEY_TOKEN=$(echo "$resp" | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
  echo "PASS journey admin_login"
}

journey_runtime_auth_failed() {
  local resp
  resp=$(curl -s -X POST "$ADMIN_URL/v1/llm/invoke" \
    -H "Authorization: Bearer wrong-token" \
    -H "Content-Type: application/json" \
    -d '{"appKey":"order-service","code":"aiChat","inputs":{"question":"hi"}}')
  echo "$resp" | grep -q "AUTH_FAILED" || { echo "FAIL journey runtime_auth_failed: $resp"; return 1; }
  echo "PASS journey runtime_auth_failed"
}

journey_admin_execution_list() {
  [ -n "$JOURNEY_TOKEN" ] || journey_admin_login
  local resp
  resp=$(curl -s -H "Authorization: Bearer $JOURNEY_TOKEN" "$ADMIN_URL/api/admin/executions?page=1&size=10")
  echo "$resp" | grep -q "records" || echo "$resp" | grep -q "total" || { echo "FAIL journey admin_execution_list: $resp"; return 1; }
  echo "PASS journey admin_execution_list"
}

journey_admin_adapter_health_all() {
  [ -n "$JOURNEY_TOKEN" ] || journey_admin_login
  local resp
  resp=$(curl -s -H "Authorization: Bearer $JOURNEY_TOKEN" "$ADMIN_URL/api/admin/adapters/health/all")
  echo "$resp" | grep -q "adapterId" || { echo "FAIL journey admin_adapter_health_all: $resp"; return 1; }
  echo "PASS journey admin_adapter_health_all"
}

journey_admin_audit_logs() {
  [ -n "$JOURNEY_TOKEN" ] || journey_admin_login
  local resp
  resp=$(curl -s -H "Authorization: Bearer $JOURNEY_TOKEN" "$ADMIN_URL/api/admin/audit-logs?page=1&size=20")
  echo "$resp" | grep -q "records" || echo "$resp" | grep -q "total" || { echo "FAIL journey admin_audit_logs: $resp"; return 1; }
  echo "PASS journey admin_audit_logs"
}

journey_admin_tenants() {
  [ -n "$JOURNEY_TOKEN" ] || journey_admin_login
  local resp
  resp=$(curl -s -H "Authorization: Bearer $JOURNEY_TOKEN" "$ADMIN_URL/api/admin/tenants")
  echo "$resp" | grep -q "zest-demo" || echo "$resp" | grep -q "tenantCode" || { echo "FAIL journey admin_tenants: $resp"; return 1; }
  echo "PASS journey admin_tenants"
}

journey_registry_methods_optional() {
  [ -n "$JOURNEY_TOKEN" ] || journey_admin_login
  local resp
  resp=$(curl -s -H "Authorization: Bearer $JOURNEY_TOKEN" "$ADMIN_URL/api/admin/registry/methods?page=1&size=20")
  echo "$resp" | grep -q "records" || echo "$resp" | grep -q "total" || { echo "FAIL journey registry_methods_optional: $resp"; return 1; }
  echo "PASS journey registry_methods_optional"
}

journey_flow_llm_invoke() {
  local resp
  resp=$(curl -s -X POST "$ADMIN_URL/v1/llm/invoke" \
    -H "Authorization: Bearer demo-token-123" \
    -H "Content-Type: application/json" \
    -d '{"appKey":"order-service","code":"aiChat","inputs":{"question":"flow-node"},"context":{"flowId":"demo-flow"}}')
  echo "$resp" | grep -q "traceId" && echo "PASS journey flow_llm_invoke" || { echo "FAIL journey flow_llm_invoke: $resp"; return 1; }
}

journey_prompt_diff() {
  [ -n "$JOURNEY_TOKEN" ] || journey_admin_login
  local resp
  resp=$(curl -s -H "Authorization: Bearer $JOURNEY_TOKEN" "$ADMIN_URL/api/admin/prompts/aiChat/diff?from=v1&to=v2")
  echo "$resp" | grep -q "changes" && echo "PASS journey prompt_diff" || { echo "FAIL journey prompt_diff: $resp"; return 1; }
}

journey_operator_readonly() {
  local token
  token=$(curl -s -X POST "$ADMIN_URL/api/admin/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"operator","password":"operator123"}' | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
  [ -n "$token" ] || { echo "FAIL journey operator_readonly: login"; return 1; }
  local list
  list=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $token" "$ADMIN_URL/api/admin/tasks?page=1&size=5")
  [ "$list" = "200" ] || { echo "FAIL journey operator_readonly list=$list"; return 1; }
  local forbidden
  forbidden=$(curl -s -o /dev/null -w "%{http_code}" -X POST -H "Authorization: Bearer $token" \
    -H "Content-Type: application/json" \
    "$ADMIN_URL/api/admin/prompts/aiChat/publish" -d '{"version":"v1"}')
  [ "$forbidden" = "403" ] && echo "PASS journey operator_readonly" || { echo "FAIL journey operator_readonly publish=$forbidden"; return 1; }
}

journey_agent_profile_diff() {
  [ -n "$JOURNEY_TOKEN" ] || journey_admin_login
  local resp
  resp=$(curl -s -H "Authorization: Bearer $JOURNEY_TOKEN" "$ADMIN_URL/api/admin/agent-profiles/aiChat/diff?from=v1&to=v1")
  echo "$resp" | grep -q "changes" && echo "PASS journey agent_profile_diff" || { echo "FAIL journey agent_profile_diff: $resp"; return 1; }
}

journey_admin_users() {
  [ -n "$JOURNEY_TOKEN" ] || journey_admin_login
  local resp
  resp=$(curl -s -H "Authorization: Bearer $JOURNEY_TOKEN" "$ADMIN_URL/api/admin/users")
  echo "$resp" | grep -q "admin" && echo "PASS journey admin_users" || { echo "FAIL journey admin_users: $resp"; return 1; }
}

journey_mcp_tools_list() {
  [ -n "$JOURNEY_TOKEN" ] || journey_admin_login
  local resp
  resp=$(curl -s -H "Authorization: Bearer $JOURNEY_TOKEN" "$ADMIN_URL/api/admin/mcp-servers/e2e-docs/tools")
  echo "$resp" | grep -q "search" && echo "PASS journey mcp_tools_list" || { echo "FAIL journey mcp_tools_list: $resp"; return 1; }
}

journey_cp_invoke_stream() {
  local resp
  resp=$(curl -s -N -X POST "$ADMIN_URL/v1/llm/invoke/stream" \
    -H "Authorization: Bearer demo-token-123" \
    -H "Content-Type: application/json" \
    -H "Accept: text/event-stream" \
    -d '{"appKey":"order-service","code":"aiChat","inputs":{"question":"journey-stream"}}' | head -n 3)
  echo "$resp" | grep -q "meta" && echo "PASS journey cp_invoke_stream" || { echo "FAIL journey cp_invoke_stream: $resp"; return 1; }
}

FLOW_URL="${FLOW_URL:-http://localhost:20552}"

journey_zestflow_ai_chat_chain() {
  local resp
  resp=$(curl -s -X POST "$FLOW_URL/api/execute" \
    -H "Content-Type: application/json" \
    -d '{"chainCode":"CHN_ZESTLLM_AI_CHAT","params":{"question":"journey-flow-dag","bearerToken":"demo-token-123"}}')
  echo "$resp" | grep -q "traceId" && echo "PASS journey zestflow_ai_chat_chain" || { echo "FAIL journey zestflow_ai_chat_chain: $resp"; return 1; }
}

journey_zestflow_invoke_audit_chain() {
  local resp
  resp=$(curl -s -X POST "$FLOW_URL/api/execute" \
    -H "Content-Type: application/json" \
    -d '{"chainCode":"CHN_ZESTLLM_INVOKE_AUDIT","params":{"question":"journey-audit-dag","bearerToken":"demo-token-123"}}')
  echo "$resp" | grep -q "traceId" && echo "PASS journey zestflow_invoke_audit_chain" || { echo "FAIL journey zestflow_invoke_audit_chain: $resp"; return 1; }
}

journey_flow_gateway_ai_chat() {
  local resp
  resp=$(curl -s -X POST "$ADMIN_URL/api/flow/ai-chat" \
    -H "Content-Type: application/json" \
    -d '{"question":"journey-gateway","bearerToken":"demo-token-123"}')
  echo "$resp" | grep -q "traceId" && echo "PASS journey flow_gateway_ai_chat" || { echo "FAIL journey flow_gateway_ai_chat: $resp"; return 1; }
}

journey_ai_chat_tools_prepare() {
  local resp
  resp=$(curl -s -X POST "$ADMIN_URL/v1/llm/prepare" \
    -H "Authorization: Bearer demo-token-123" \
    -H "Content-Type: application/json" \
    -d '{"appKey":"order-service","code":"aiChatTools","inputs":{"question":"tools"}}')
  echo "$resp" | grep -q "toolCallMode" && echo "PASS journey ai_chat_tools_prepare" || { echo "FAIL journey ai_chat_tools_prepare: $resp"; return 1; }
}

journey_demo_flow_chat() {
  local resp
  resp=$(curl -s "$DEMO_URL/demo/order/flowChat?orderId=1&question=journey-demo-flow")
  echo "$resp" | grep -q "traceId" && echo "PASS journey demo_flow_chat" || { echo "FAIL journey demo_flow_chat: $resp"; return 1; }
}

echo "== Running journeys from $ROOT_DIR/.zestflow/acceptance/journeys.yml =="
journey_admin_login
journey_runtime_auth_failed
journey_flow_llm_invoke
journey_admin_execution_list
journey_admin_adapter_health_all
journey_admin_audit_logs
journey_admin_tenants
journey_registry_methods_optional
journey_prompt_diff
journey_agent_profile_diff
journey_admin_users
journey_mcp_tools_list
journey_cp_invoke_stream
journey_zestflow_ai_chat_chain
journey_zestflow_invoke_audit_chain
journey_flow_gateway_ai_chat
journey_ai_chat_tools_prepare
journey_demo_flow_chat
journey_operator_readonly
echo "== All journeys passed =="
