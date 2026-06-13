#!/usr/bin/env bash
# 等待 compose 栈就绪（Admin + Demo + 可选 Flow）
set -euo pipefail

ADMIN_URL="${ADMIN_URL:-http://localhost:8088}"
DEMO_URL="${DEMO_URL:-http://localhost:8081}"
FLOW_URL="${FLOW_URL:-http://localhost:20552}"
MAX_RETRIES="${MAX_RETRIES:-90}"
SLEEP_SEC="${SLEEP_SEC:-5}"

echo "== Waiting for stack (max ${MAX_RETRIES}x${SLEEP_SEC}s) =="
echo "  Admin: $ADMIN_URL"
echo "  Demo:  $DEMO_URL"

for i in $(seq 1 "$MAX_RETRIES"); do
  admin_ok=0
  demo_ok=0
  curl -sf "$ADMIN_URL/swagger-ui.html" >/dev/null 2>&1 && admin_ok=1
  curl -sf "$DEMO_URL/demo/order/methodA?orderId=1&question=ready" >/dev/null 2>&1 && demo_ok=1
  if [ "$admin_ok" = "1" ] && [ "$demo_ok" = "1" ]; then
    echo "Stack ready after ${i} attempt(s)"
    exit 0
  fi
  echo "  attempt $i/$MAX_RETRIES admin=$admin_ok demo=$demo_ok"
  sleep "$SLEEP_SEC"
done

echo "FAIL: stack not ready"
exit 1
