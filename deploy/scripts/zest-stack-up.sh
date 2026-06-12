#!/usr/bin/env bash
# Zest Stack 分层一键部署
# 用法: bash zest-stack-up.sh [small|medium|large]
set -euo pipefail

TIER="${1:-small}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "== Zest Stack Tier: $TIER =="
docker compose up -d mysql
sleep 8

case "$TIER" in
  medium)
    docker compose \
      -f docker-compose.yml \
      -f docker-compose.observability.yml \
      -f docker-compose.plan-a.yml \
      --profile observability \
      up -d --build
    ;;
  large)
    docker compose \
      -f docker-compose.yml \
      -f docker-compose.observability.yml \
      -f docker-compose.plan-a.yml \
      -f docker-compose.kafka.yml \
      -f docker-compose.integration.yml \
      --profile observability \
      --profile kafka \
      --profile integration \
      up -d --build
    ;;
  *)
    docker compose up -d --build
    ;;
esac

echo ""
echo "Zest Stack ($TIER) started."
echo "  Admin+UI  http://localhost:8088  (admin/admin123)"
echo "  Demo      http://localhost:8081"
echo ""
echo "E2E: bash deploy/scripts/e2e-acceptance.sh"
echo "Stress: powershell -File deploy/scripts/stress-test-prepare.ps1 -Concurrency 50"
