#!/usr/bin/env bash
# ZestLLM 生产级全量验收编排（Linux / Docker 全栈）
# 五阶段：白盒 → 黑盒(e2e) → SSO 冒烟 → 链路(journeys) → 压测(loadtest)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

TIER="${TIER:-production}"
ADMIN_URL="${ADMIN_URL:-http://localhost:8088}"
REPORT_DIR="${REPORT_DIR:-$ROOT/deploy/test-reports}"
mkdir -p "$REPORT_DIR"
MASTER="$REPORT_DIR/production-$(date +%Y%m%d-%H%M%S).txt"

log() { echo "[$(date +%H:%M:%S)] $*" | tee -a "$MASTER"; }

P95_MAX="${P95_MAX:-500}"
if [ "$TIER" = "local" ]; then
  P95_MAX=800
fi

log "=== ZestLLM Production Acceptance tier=$TIER ==="

log "======== PHASE: WHITEBOX mvn test ========"
mvn -B test 2>&1 | tee "$REPORT_DIR/mvn-test-latest.log"
log "PASS WHITEBOX"

log "======== PHASE: WHITEBOX *IT (Testcontainers, optional) ========"
if [ "${SKIP_IT:-0}" = "1" ]; then
  log "SKIP *IT (SKIP_IT=1)"
else
  if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    mvn -B test -pl zest-llm-admin -Dtest='*IT' -Dsurefire.failIfNoSpecifiedTests=false \
      2>&1 | tee "$REPORT_DIR/mvn-it-latest.log" || {
        log "WARN *IT failed — check Docker/Testcontainers"
        exit 1
      }
    log "PASS *IT"
  else
    log "SKIP *IT (Docker unavailable)"
  fi
fi

log "======== PHASE: BLACKBOX e2e-acceptance AC1-38 ========"
ADMIN_URL="$ADMIN_URL" bash deploy/scripts/e2e-acceptance.sh 2>&1 | tee -a "$MASTER"
log "PASS BLACKBOX e2e"

log "======== PHASE: SSO sso-smoke ========"
if [ "${SKIP_SSO:-0}" = "1" ]; then
  log "SKIP SSO (SKIP_SSO=1)"
else
  ADMIN_URL="$ADMIN_URL" SSO_BASE="${SSO_BASE:-http://localhost:9000}" \
    bash deploy/scripts/sso-smoke.sh 2>&1 | tee -a "$MASTER"
  log "PASS SSO sso-smoke"
fi

log "======== PHASE: CHAIN run-journeys ========"
bash deploy/scripts/run-journeys.sh 2>&1 | tee -a "$MASTER"
log "PASS CHAIN journeys"

log "======== PHASE: STRESS loadtest-cp-prepare ========"
P95_MAX_MS="$P95_MAX" REQUESTS="${REQUESTS:-200}" CONCURRENCY="${CONCURRENCY:-30}" \
  bash deploy/scripts/loadtest-cp-prepare.sh 2>&1 | tee -a "$MASTER"
log "PASS STRESS"

log "======== PRODUCTION GATES ========"
log "PASS GATE-WB mvn test"
log "PASS GATE-BB e2e-acceptance AC1-38"
if [ "${SKIP_SSO:-0}" = "1" ]; then
  log "SKIP GATE-SSO sso-smoke"
else
  log "PASS GATE-SSO sso-smoke"
fi
log "PASS GATE-CH run-journeys"
log "PASS GATE-ST P95<=${P95_MAX}ms"
log "MasterReport=$MASTER"
log "=== ALL PRODUCTION ACCEPTANCE PASSED ==="
