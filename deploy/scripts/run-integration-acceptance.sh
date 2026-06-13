#!/usr/bin/env bash
# B 整合栈验收：严格模式 AC40–42 / AC55–56 + INTEGRATION 块
# 前置：large 栈 + integration profile 已启动
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

export ZEST_INTEGRATION_E2E=1
export ADMIN_URL="${ADMIN_URL:-http://localhost:8088}"

echo "== B Integration Acceptance (ZEST_INTEGRATION_E2E=1) =="
echo "Admin: $ADMIN_URL"
echo ""

bash deploy/scripts/e2e-acceptance.sh

echo ""
echo "== Optional: full B demo smoke =="
bash deploy/scripts/integration-demo.sh
