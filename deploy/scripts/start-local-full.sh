#!/usr/bin/env bash
# ZestLLM 本地完整版一键启动（macOS / Linux）
# 用法: bash deploy/scripts/start-local-full.sh [--with-litellm] [--embed-ui] [--skip-build] [--stop-only]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DEPLOY="$ROOT/deploy"
ADMIN_DIR="$ROOT/zest-llm-admin"
UI_DIR="$ROOT/zest-llm-admin-ui"
LOCAL_YML="$ADMIN_DIR/src/main/resources/application-local.yml"
EXAMPLE_YML="$ADMIN_DIR/src/main/resources/application-local.example.yml"
JAR="$ADMIN_DIR/target/zest-llm-admin-1.0.0.jar"
LOG_DIR="$ROOT/deploy/logs"
PID_DIR="$LOG_DIR/pids"
ADMIN_LOG="$LOG_DIR/admin-local.log"
ADMIN_ERR_LOG="$LOG_DIR/admin-local.err.log"
UI_LOG="$LOG_DIR/admin-ui-dev.log"
ADMIN_PID_FILE="$PID_DIR/admin-local.pid"
UI_PID_FILE="$PID_DIR/admin-ui-dev.pid"

WITH_LITELLM=false
EMBED_UI=false
SKIP_BUILD=false
STOP_ONLY=false

for arg in "$@"; do
  case "$arg" in
    --with-litellm) WITH_LITELLM=true ;;
    --embed-ui) EMBED_UI=true ;;
    --skip-build) SKIP_BUILD=true ;;
    --stop-only) STOP_ONLY=true ;;
    *) echo "Unknown option: $arg" >&2; exit 1 ;;
  esac
done

stop_from_pid_file() {
  local file="$1"
  local label="$2"
  if [[ -f "$file" ]]; then
    local pid
    pid="$(cat "$file" 2>/dev/null || true)"
    if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
      echo "Stopping $label PID $pid"
      kill "$pid" 2>/dev/null || true
      sleep 1
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$file"
  fi
}

wait_http_ok() {
  local url="$1"
  local retries="${2:-60}"
  for ((i=1; i<=retries; i++)); do
    if curl -sf -o /dev/null -w '' "$url" 2>/dev/null \
      || curl -sf -o /dev/null -w '' "$url" 2>/dev/null; then
      return 0
    fi
    sleep 2
  done
  return 1
}

if $STOP_ONLY; then
  stop_from_pid_file "$ADMIN_PID_FILE" "Admin"
  stop_from_pid_file "$UI_PID_FILE" "Admin UI dev"
  echo "Stopped local stack (PID files)."
  exit 0
fi

mkdir -p "$LOG_DIR" "$PID_DIR"

if [[ ! -f "$LOCAL_YML" ]]; then
  [[ -f "$EXAMPLE_YML" ]] || { echo "Missing $EXAMPLE_YML" >&2; exit 1; }
  cp "$EXAMPLE_YML" "$LOCAL_YML"
  echo "Created application-local.yml from example — edit MySQL password if needed."
fi

if ! (echo >/dev/tcp/127.0.0.1/3306) 2>/dev/null; then
  if command -v docker >/dev/null 2>&1; then
    echo "== Starting MySQL via Docker =="
    (cd "$DEPLOY" && docker compose up -d mysql)
    sleep 8
  else
    echo "MySQL :3306 not reachable and Docker not found." >&2
    exit 1
  fi
fi

if $WITH_LITELLM; then
  command -v docker >/dev/null 2>&1 || { echo "--with-litellm requires Docker" >&2; exit 1; }
  echo "== Starting LiteLLM + openai-mock =="
  (cd "$DEPLOY" && docker compose up -d openai-mock litellm)
fi

if ! $SKIP_BUILD; then
  echo "== mvn package (skip tests) =="
  (cd "$ROOT" && mvn -pl zest-llm-admin -am package -DskipTests -q)
fi

if $EMBED_UI; then
  bash "$(dirname "$0")/build-admin-ui.sh"
  (cd "$ROOT" && mvn -pl zest-llm-admin -am package -DskipTests -q)
fi

[[ -f "$JAR" ]] || { echo "Missing jar: $JAR" >&2; exit 1; }

stop_from_pid_file "$ADMIN_PID_FILE" "Admin"
stop_from_pid_file "$UI_PID_FILE" "Admin UI dev"

echo "== Starting Admin (:8088) =="
nohup java -jar "$JAR" --spring.profiles.active=local >"$ADMIN_LOG" 2>"$ADMIN_ERR_LOG" &
echo $! >"$ADMIN_PID_FILE"

if ! wait_http_ok "http://127.0.0.1:8088/swagger-ui.html"; then
  tail -n 40 "$ADMIN_LOG" || true
  echo "Admin failed to start — see $ADMIN_LOG" >&2
  exit 1
fi

if ! $EMBED_UI; then
  echo "== Starting Admin UI dev (:5174) =="
  (cd "$UI_DIR" && nohup npm run dev >"$UI_LOG" 2>&1 &)
  echo $! >"$UI_PID_FILE"
  if ! wait_http_ok "http://localhost:5174/"; then
    tail -n 30 "$UI_LOG" || true
    echo "Admin UI dev failed — see $UI_LOG" >&2
    exit 1
  fi
fi

echo ""
echo "ZestLLM local stack is up."
if $EMBED_UI; then
  echo "  Admin (embedded UI): http://127.0.0.1:8088  (login admin / admin123)"
else
  echo "  Admin UI (dev):      http://localhost:5174  (login admin / admin123)"
  echo "  Admin API:           http://127.0.0.1:8088"
fi
echo "  ZestFlow CP:         http://127.0.0.1:20552"
if $WITH_LITELLM; then
  echo "  LiteLLM:             http://127.0.0.1:4000 (Model Gateway UP)"
else
  echo "  LiteLLM:             not started (Dashboard Gateway DOWN is expected)"
fi
echo "  Logs:                $LOG_DIR"
echo ""
echo "Verify: bash deploy/scripts/verify-local.sh"
echo "Stop:   bash deploy/scripts/start-local-full.sh --stop-only"
