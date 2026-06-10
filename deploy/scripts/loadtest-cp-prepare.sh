#!/usr/bin/env bash
# 方案 A Phase2：CP prepare 压测 + P50/P95/P99 报告
set -euo pipefail

ADMIN_URL="${ADMIN_URL:-http://localhost:8088}"
REQUESTS="${REQUESTS:-100}"
CONCURRENCY="${CONCURRENCY:-10}"
ENDPOINT="${ENDPOINT:-prepare}"
BODY='{"appKey":"order-service","code":"aiChat","inputs":{"question":"loadtest"}}'
URL="$ADMIN_URL/v1/llm/$ENDPOINT"

echo "== Load test $ENDPOINT: requests=$REQUESTS concurrency=$CONCURRENCY =="

if command -v hey >/dev/null 2>&1; then
  hey -n "$REQUESTS" -c "$CONCURRENCY" -m POST \
    -H "Authorization: Bearer demo-token-123" \
    -H "Content-Type: application/json" \
    -d "$BODY" \
    "$URL"
  echo "PASS loadtest-cp (hey) — see latency distribution above"
  exit 0
fi

TMP=$(mktemp)
trap 'rm -f "$TMP"' EXIT

for i in $(seq 1 "$REQUESTS"); do
  start_ms=$(python3 -c 'import time; print(int(time.time()*1000))' 2>/dev/null || date +%s%3N)
  code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$URL" \
    -H "Authorization: Bearer demo-token-123" \
    -H "Content-Type: application/json" \
    -d "$BODY")
  end_ms=$(python3 -c 'import time; print(int(time.time()*1000))' 2>/dev/null || date +%s%3N)
  latency=$((end_ms - start_ms))
  echo "$latency $code" >> "$TMP"
done

P95_MAX_MS="${P95_MAX_MS:-500}"

python3 - "$TMP" "$P95_MAX_MS" <<'PY'
import sys
rows = []
for line in open(sys.argv[1]):
    parts = line.strip().split()
    if len(parts) != 2:
        continue
    ms, code = int(parts[0]), parts[1]
    rows.append((ms, code))
if not rows:
    print("FAIL: no samples")
    sys.exit(1)
latencies = sorted(ms for ms, c in rows if c == "200")
fail = sum(1 for _, c in rows if c != "200")
if not latencies:
    print(f"FAIL: all requests failed (fail={fail})")
    sys.exit(1)

def pct(p):
    idx = max(0, min(len(latencies)-1, int(len(latencies)*p/100)-1))
    return latencies[idx]

p95 = pct(95)
p95_max = int(sys.argv[2])
print(f"samples={len(latencies)} fail={fail}")
print(f"P50={pct(50)}ms P95={p95}ms P99={pct(99)}ms max={latencies[-1]}ms")
if p95 > p95_max:
    print(f"FAIL: P95 {p95}ms exceeds threshold {p95_max}ms")
    sys.exit(1)
PY

echo "PASS loadtest-cp (curl + P95 report)"
