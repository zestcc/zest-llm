#!/usr/bin/env bash
# ZestStory × ZestLLM 联调冒烟（Linux / Docker CI）
# 用例：E2E-01 zestStoryInvoke · RAG-01 zestStoryRag · E2E-02（SKIP，栈内无 ZestStory）
set -euo pipefail

ADMIN_URL="${ADMIN_URL:-http://localhost:8088}"
TOKEN="${ZESTSTORY_TOKEN:-zeststory-runtime-dev-token}"
PASS=0
FAIL=0
SKIP=0

pass_case() {
  echo "PASS $1 $2"
  PASS=$((PASS + 1))
}

fail_case() {
  echo "FAIL $1 $2"
  FAIL=$((FAIL + 1))
}

skip_case() {
  echo "SKIP $1 $2"
  SKIP=$((SKIP + 1))
}

echo "== E2E-01: zestStoryInvoke =="
BODY='{"appKey":"zeststory","code":"zestStoryInvoke","inputs":{"systemPrompt":"novel assistant","userMessage":"write one opening line"}}'
RESP=$(curl -sf -X POST "$ADMIN_URL/v1/llm/invoke" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json; charset=utf-8" \
  -d "$BODY" 2>&1) || RESP=""
if echo "$RESP" | grep -q '"status"[[:space:]]*:[[:space:]]*"SUCCESS"' \
  && echo "$RESP" | grep -q '"answer"'; then
  TRACE=$(echo "$RESP" | sed -n 's/.*"traceId"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
  pass_case "E2E-01" "zestStoryInvoke trace=${TRACE:-unknown}"
else
  fail_case "E2E-01" "zestStoryInvoke resp=${RESP:-empty}"
fi

echo "== RAG-01: zestStoryRag =="
BODY='{"appKey":"zeststory","code":"zestStoryRag","inputs":{"systemPrompt":"novel assistant","userMessage":"continue from lore"}}'
RESP=$(curl -sf -X POST "$ADMIN_URL/v1/llm/invoke" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json; charset=utf-8" \
  -d "$BODY" 2>&1) || RESP=""
if echo "$RESP" | grep -q '"status"[[:space:]]*:[[:space:]]*"SUCCESS"'; then
  TRACE=$(echo "$RESP" | sed -n 's/.*"traceId"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
  pass_case "RAG-01" "zestStoryRag trace=${TRACE:-unknown}"
else
  fail_case "RAG-01" "zestStoryRag resp=${RESP:-empty}"
fi

skip_case "E2E-02" "zestory not in docker stack (run locally with ZestStory :8080)"

echo ""
echo "SUMMARY: $PASS PASS / $FAIL FAIL / $SKIP SKIP"
if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
