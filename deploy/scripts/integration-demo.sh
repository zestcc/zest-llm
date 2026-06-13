#!/usr/bin/env bash
# B 整合栈 Demo：RAGFlow 知识 + Dify 编排 + 场景向导 smoke
# 前置：bash deploy/scripts/zest-stack-up.sh large && bash deploy/scripts/wait-stack-ready.sh
set -euo pipefail

ADMIN_URL="${ADMIN_URL:-http://localhost:8088}"
DEMO_TOKEN="${DEMO_TOKEN:-demo-token-123}"
APP_KEY="${APP_KEY:-order-service}"

PASS=0
FAIL=0

pass() { echo "PASS $1"; PASS=$((PASS + 1)); }
fail() { echo "FAIL $1: $2"; FAIL=$((FAIL + 1)); }

echo "== B Integration Demo (Admin=$ADMIN_URL) =="

TOKEN=$(curl -sf -X POST "$ADMIN_URL/api/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then
  echo "FAIL login — is Admin up?"
  exit 1
fi
pass "login"

ADAPTERS=$(curl -sf -H "Authorization: Bearer $TOKEN" "$ADMIN_URL/api/admin/adapters/health/all" || echo "{}")
echo "$ADAPTERS" | grep -qi "ragflow" && pass "adapter-ragflow-listed" \
  || fail "adapter-ragflow-listed" "ragflow not in health (sidecar may be down — degraded OK for smoke)"
echo "$ADAPTERS" | grep -qi "dify" && pass "adapter-dify-listed" \
  || fail "adapter-dify-listed" "dify not in health (sidecar may be down — degraded OK for smoke)"

echo "== Wizard: report-basic -> aiReport =="
WZ_RPT=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/ai-jobs/wizard" \
  -d "{\"templateId\":\"report-basic\",\"appKey\":\"$APP_KEY\",\"taskCode\":\"aiReport\",\"publish\":true,\"runProbe\":false}")
echo "$WZ_RPT" | grep -q "aiReport" && pass "wizard-aiReport" || fail "wizard-aiReport" "$WZ_RPT"

echo "== Wizard: ops-monitor -> aiOps =="
WZ_OPS=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "$ADMIN_URL/api/admin/ai-jobs/wizard" \
  -d "{\"templateId\":\"ops-monitor\",\"appKey\":\"$APP_KEY\",\"taskCode\":\"aiOps\",\"publish\":true,\"runProbe\":false}")
echo "$WZ_OPS" | grep -q "aiOps" && pass "wizard-aiOps" || fail "wizard-aiOps" "$WZ_OPS"

echo "== Smoke: prepare + invoke aiReport (hybrid ragflow) =="
PREP_RPT=$(curl -s -X POST "$ADMIN_URL/v1/llm/prepare" \
  -H "Authorization: Bearer $DEMO_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"appKey\":\"$APP_KEY\",\"code\":\"aiReport\",\"inputs\":{\"question\":\"解读 Q4 报表趋势\"}}")
if echo "$PREP_RPT" | grep -q "knowledgePrefetch"; then
  pass "prepare-aiReport-knowledgePrefetch"
else
  fail "prepare-aiReport-knowledgePrefetch" "$PREP_RPT"
fi
INV_RPT=$(curl -s -X POST "$ADMIN_URL/v1/llm/invoke" \
  -H "Authorization: Bearer $DEMO_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"appKey\":\"$APP_KEY\",\"code\":\"aiReport\",\"inputs\":{\"question\":\"解读 Q4 报表趋势\"}}")
echo "$INV_RPT" | grep -q "traceId" && pass "invoke-aiReport" || fail "invoke-aiReport" "$INV_RPT"

echo "== Smoke: prepare + invoke aiOps (dify + ragflow profile) =="
PREP_OPS=$(curl -s -X POST "$ADMIN_URL/v1/llm/prepare" \
  -H "Authorization: Bearer $DEMO_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"appKey\":\"$APP_KEY\",\"code\":\"aiOps\",\"inputs\":{\"question\":\"pod CrashLoopBackOff 诊断\"}}")
echo "$PREP_OPS" | grep -q "profileVersion" && pass "prepare-aiOps" || fail "prepare-aiOps" "$PREP_OPS"
INV_OPS=$(curl -s -X POST "$ADMIN_URL/v1/llm/invoke" \
  -H "Authorization: Bearer $DEMO_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"appKey\":\"$APP_KEY\",\"code\":\"aiOps\",\"inputs\":{\"question\":\"pod CrashLoopBackOff 诊断\"}}")
echo "$INV_OPS" | grep -q "traceId" && pass "invoke-aiOps" || fail "invoke-aiOps" "$INV_OPS"

echo ""
echo "== B Integration Demo Summary: PASS=$PASS FAIL=$FAIL =="
if [ "$FAIL" -gt 0 ]; then
  echo "Some checks failed — Dify/RAGFlow sidecars may be unreachable (see adapter health)."
  exit 1
fi
echo "All B integration demo checks passed."
exit 0
