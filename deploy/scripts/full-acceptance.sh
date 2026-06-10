#!/usr/bin/env bash
# 生产级全量验收入口（Linux / Docker 环境）
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

echo "== Phase 1: Unit tests (*Test) =="
mvn -q test -pl zest-llm-infra,zest-llm-agent,zest-llm-admin -am -Dtest='*Test' -Dsurefire.failIfNoSpecifiedTests=false

echo "== Phase 2: E2E AC1-38 =="
bash deploy/scripts/e2e-acceptance.sh

echo "== Phase 3: Journeys =="
bash deploy/scripts/run-journeys.sh

echo "== Phase 4: Performance smoke =="
bash deploy/scripts/loadtest-cp-prepare.sh

echo "== All full acceptance phases passed =="
