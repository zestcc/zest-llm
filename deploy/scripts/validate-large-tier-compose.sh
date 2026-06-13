#!/usr/bin/env bash
# Large Tier compose 配置干跑校验（不启动容器）
# 用法: bash deploy/scripts/validate-large-tier-compose.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if ! command -v docker >/dev/null 2>&1; then
  echo "FAIL: docker CLI not found" >&2
  exit 1
fi

echo "== Validate Large Tier compose config (dry-run) =="

docker compose \
  -f docker-compose.yml \
  -f docker-compose.observability.yml \
  -f docker-compose.plan-a.yml \
  -f docker-compose.kafka.yml \
  -f docker-compose.integration.yml \
  --profile observability \
  --profile kafka \
  --profile integration \
  config --quiet

echo "PASS validate-large-tier-compose — merged config OK"
echo "Profiles: observability + kafka + integration"
echo "Components: medium stack + Kafka/Redpanda + Dify + RAGFlow"
echo "Start: bash deploy/scripts/zest-stack-up.sh large"
echo "B Demo: bash deploy/scripts/integration-demo.sh"
