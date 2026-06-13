#!/usr/bin/env bash
# ZestLLM Admin SSO 联调冒烟脚本（Linux / macOS）
# 用法：bash deploy/scripts/sso-smoke.sh
# 环境变量：ADMIN_URL（默认 http://localhost:8088）、SSO_BASE（默认 http://localhost:9000）
set -euo pipefail

ADMIN_URL="${ADMIN_URL:-http://localhost:8088}"
SSO_BASE="${SSO_BASE:-http://localhost:9000}"

echo "== ZestLLM Admin SSO Smoke =="
echo "Admin: $ADMIN_URL"
echo "SSO:   $SSO_BASE"

# --- [1] OIDC Discovery（ZestSSO 未启动时 WARN，不阻断）---
echo ""
echo "[1] OIDC Discovery"
DISCOVERY_URL="$SSO_BASE/api/public/.well-known/openid-configuration"
if discovery=$(curl -sf --max-time 10 "$DISCOVERY_URL" 2>/dev/null); then
  if echo "$discovery" | grep -q '"authorization_endpoint"'; then
    auth_ep=$(echo "$discovery" | grep -o '"authorization_endpoint"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1)
    echo "OK $auth_ep"
  else
    echo "WARN Discovery 缺少 authorization_endpoint"
  fi
else
  echo "WARN ZestSSO 未启动或 Discovery 不可达: $DISCOVERY_URL"
fi

# --- [2] Admin SSO Config ---
echo ""
echo "[2] Admin SSO Config"
config_json=""
if config_json=$(curl -sf --max-time 10 "$ADMIN_URL/api/admin/auth/sso/config" 2>/dev/null); then
  :
elif config_json=$(curl -sf --max-time 10 "$ADMIN_URL/api/admin/auth/oidc/config" 2>/dev/null); then
  echo "WARN /sso/config 不可用，回退 /oidc/config"
else
  echo "FAIL Admin SSO config 不可达" >&2
  exit 1
fi
enabled=$(echo "$config_json" | grep -o '"enabled"[[:space:]]*:[[:space:]]*true' || true)
provider=$(echo "$config_json" | grep -o '"provider"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 || true)

if [ -z "$enabled" ]; then
  echo "SKIP zest-llm.admin.sso.enabled=false，跳过 authorize 步骤"
  SSO_ENABLED=false
else
  SSO_ENABLED=true
  echo "OK $provider"
fi

# --- [3] Admin SSO Authorize (PKCE) — 仅 enabled=true 时硬失败 ---
echo ""
echo "[3] Admin SSO Authorize (PKCE)"
if [ "$SSO_ENABLED" = true ]; then
  auth_json=""
  if auth_json=$(curl -sf --max-time 10 "$ADMIN_URL/api/admin/auth/sso/authorize" 2>/dev/null); then
    :
  elif auth_json=$(curl -sf --max-time 10 "$ADMIN_URL/api/admin/auth/oidc/authorize" 2>/dev/null); then
    echo "WARN /sso/authorize 不可用，回退 /oidc/authorize"
  else
    echo "FAIL authorize 不可达（sso.enabled=true）" >&2
    exit 1
  fi
  if echo "$auth_json" | grep -q '"authorizationUrl"' && echo "$auth_json" | grep -q '"state"'; then
    state=$(echo "$auth_json" | grep -o '"state"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1)
    echo "OK $state"
  else
    echo "FAIL authorize 响应不完整（sso.enabled=true）" >&2
    exit 1
  fi
else
  echo "SKIP (sso.enabled=false)"
fi

# --- [4] Legacy OIDC config alias ---
echo ""
echo "[4] Legacy OIDC config alias"
oidc_json=$(curl -sf --max-time 10 "$ADMIN_URL/api/admin/auth/oidc/config")
oidc_provider=$(echo "$oidc_json" | grep -o '"provider"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 || true)
oidc_enabled=$(echo "$oidc_json" | grep -o '"enabled"[[:space:]]*:[[:space:]]*[a-z]*' | head -1 || true)
echo "OK $oidc_provider $oidc_enabled"

echo ""
echo "== 完成 =="
echo "手工步骤：浏览器打开登录页 -> SSO 登录 -> 检查 llm_admin_user.sso_subject 已写入"
