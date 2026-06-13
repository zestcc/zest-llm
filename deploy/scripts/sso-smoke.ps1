# ZestLLM Admin SSO 联调冒烟脚本（Windows PowerShell）
# 用法：powershell -File deploy/scripts/sso-smoke.ps1 -AdminUrl http://localhost:8088 -SsoBase http://localhost:9000

param(
    [string]$AdminUrl = "http://localhost:8088",
    [string]$SsoBase = "http://localhost:9000"
)

$ErrorActionPreference = "Stop"

Write-Host "== ZestLLM Admin SSO Smoke ==" -ForegroundColor Cyan
Write-Host "Admin: $AdminUrl"
Write-Host "SSO:   $SsoBase"

Write-Host "`n[1] OIDC Discovery" -ForegroundColor Yellow
try {
    $discovery = Invoke-RestMethod -Uri "$SsoBase/api/public/.well-known/openid-configuration" -Method Get -TimeoutSec 10
    if (-not $discovery.authorization_endpoint) {
        throw "Discovery 缺少 authorization_endpoint"
    }
    Write-Host "OK authorization_endpoint=$($discovery.authorization_endpoint)"
} catch {
    Write-Host "WARN ZestSSO 未启动或 Discovery 不可达: $_" -ForegroundColor DarkYellow
}

Write-Host "`n[2] Admin SSO Config" -ForegroundColor Yellow
$config = Invoke-RestMethod -Uri "$AdminUrl/api/admin/auth/sso/config" -Method Get -TimeoutSec 10
$cfg = $config.data
if (-not $cfg) { $cfg = $config }
if (-not $cfg.enabled) {
    Write-Host "SKIP zest-llm.admin.sso.enabled=false，跳过 authorize 步骤" -ForegroundColor DarkYellow
} else {
    Write-Host "OK provider=$($cfg.provider) displayName=$($cfg.displayName)"
}

Write-Host "`n[3] Admin SSO Authorize (PKCE)" -ForegroundColor Yellow
if ($cfg.enabled) {
    $auth = Invoke-RestMethod -Uri "$AdminUrl/api/admin/auth/sso/authorize" -Method Get -TimeoutSec 10
    $authData = $auth.data
    if (-not $authData) { $authData = $auth }
    if ($authData.authorizationUrl -and $authData.state) {
        Write-Host "OK state=$($authData.state)"
    } else {
        throw "authorize 响应不完整"
    }
}

Write-Host "`n[4] Legacy OIDC config alias" -ForegroundColor Yellow
$oidcCfg = Invoke-RestMethod -Uri "$AdminUrl/api/admin/auth/oidc/config" -Method Get -TimeoutSec 10
$oidcData = $oidcCfg.data
if (-not $oidcData) { $oidcData = $oidcCfg }
Write-Host "OK provider=$($oidcData.provider) enabled=$($oidcData.enabled)"

Write-Host "`n== 完成 ==" -ForegroundColor Green
Write-Host "手工步骤：浏览器打开登录页 -> SSO 登录 -> 检查 llm_admin_user.sso_subject 已写入"
