#!/usr/bin/env bash
# 方案 A 推荐启动：全栈 + openai-mock + Langfuse 可观测
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "== Starting ZestLLM Plan A stack =="
docker compose \
  -f docker-compose.yml \
  -f docker-compose.observability.yml \
  -f docker-compose.plan-a.yml \
  --profile observability \
  up -d --build

echo ""
echo "Services:"
echo "  Admin+UI     http://localhost:8088  (admin/admin123)"
echo "  Demo         http://localhost:8081"
echo "  Langfuse     http://localhost:3000  (pk-lf-zest-demo / sk-lf-zest-demo)"
echo "  ZestFlow CP  http://localhost:20552/api/execute"
echo "  ZestFlow Demo http://localhost:20551/api/execute"
echo ""
echo "E2E: bash deploy/scripts/e2e-acceptance.sh"
