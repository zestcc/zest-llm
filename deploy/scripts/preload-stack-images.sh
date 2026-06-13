#!/usr/bin/env bash
# 预拉 Zest Stack small  tier 镜像（签字前执行，支持 LITELLM_IMAGE 等覆盖）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -f "$ROOT/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env"
  set +a
fi

LITELLM_IMAGE="${LITELLM_IMAGE:-ghcr.io/berriai/litellm:main-latest}"
WIREMOCK_IMAGE="${WIREMOCK_IMAGE:-wiremock/wiremock:3.3.1}"
MYSQL_IMAGE="${MYSQL_IMAGE:-mysql:8.0}"
VALKEY_IMAGE="${VALKEY_IMAGE:-valkey/valkey:8-alpine}"

echo "== Preload stack images =="
echo "  MYSQL=$MYSQL_IMAGE"
echo "  VALKEY=$VALKEY_IMAGE"
echo "  WIREMOCK=$WIREMOCK_IMAGE"
echo "  LITELLM=$LITELLM_IMAGE"

docker pull "$MYSQL_IMAGE"
docker pull "$VALKEY_IMAGE"
docker pull "$WIREMOCK_IMAGE"
docker pull "$LITELLM_IMAGE"

echo "OK — run: bash deploy/scripts/run-production-signoff.sh small"
