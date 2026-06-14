#!/usr/bin/env bash
# Linux/Docker 生产签字一键验收（四阶段 + 报告）
# 用法:
#   bash deploy/scripts/run-production-signoff.sh           # small tier
#   bash deploy/scripts/run-production-signoff.sh medium
#   TIER=production SKIP_IT=0 bash deploy/scripts/run-production-signoff.sh small
set -euo pipefail

STACK_TIER="${1:-small}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
REPORT_DIR="$ROOT/deploy/test-reports"
mkdir -p "$REPORT_DIR"

SIGNOFF="$REPORT_DIR/signoff-$(date +%Y%m%d-%H%M%S).txt"
exec > >(tee -a "$SIGNOFF") 2>&1

echo "=== ZestLLM Production Sign-off ==="
echo "Stack tier=$STACK_TIER"
echo "Report=$SIGNOFF"
echo "Host=$(uname -a)"
echo ""

echo "== Step 1: docker compose up (zest-stack) =="
bash "$SCRIPT_DIR/zest-stack-up.sh" "$STACK_TIER"

echo ""
echo "== Step 2: wait for health =="
bash "$SCRIPT_DIR/wait-stack-ready.sh"

echo ""
echo "== Step 3: production acceptance (TIER=production, AC1-67) =="
export TIER=production
export ADMIN_URL="${ADMIN_URL:-http://localhost:8088}"
export SKIP_IT="${SKIP_IT:-0}"
bash "$SCRIPT_DIR/production-acceptance.sh"

echo ""
echo "=== SIGN-OFF PASSED ==="
echo "Attach this file for release approval: $SIGNOFF"
