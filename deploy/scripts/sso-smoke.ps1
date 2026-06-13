# ZestLLM Admin SSO smoke (Windows PowerShell)
# Usage: powershell -File deploy/scripts/sso-smoke.ps1 -AdminUrl http://localhost:8088 -SsoBase http://localhost:9000

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
        throw "Discovery missing authorization_endpoint"
    }
    Write-Host "OK authorization_endpoint=$($discovery.authorization_endpoint)"
} catch {
    Write-Host "WARN ZestSSO not reachable: $_" -ForegroundColor DarkYellow
}

Write-Host "`n[2] Admin SSO Config" -ForegroundColor Yellow
$cfg = $null
try {
    $config = Invoke-RestMethod -Uri "$AdminUrl/api/admin/auth/sso/config" -Method Get -TimeoutSec 10
    $cfg = $config.data; if (-not $cfg) { $cfg = $config }
} catch {
    Write-Host "WARN /sso/config unavailable, fallback /oidc/config: $_" -ForegroundColor DarkYellow
    $config = Invoke-RestMethod -Uri "$AdminUrl/api/admin/auth/oidc/config" -Method Get -TimeoutSec 10
    $cfg = $config.data; if (-not $cfg) { $cfg = $config }
}
if (-not $cfg.enabled) {
    Write-Host "SKIP sso.enabled=false, skip authorize" -ForegroundColor DarkYellow
} else {
    Write-Host "OK provider=$($cfg.provider) displayName=$($cfg.displayName) enabled=$($cfg.enabled)"
}

Write-Host "`n[3] Admin SSO Authorize (PKCE)" -ForegroundColor Yellow
if ($cfg -and $cfg.enabled) {
    try {
        $auth = Invoke-RestMethod -Uri "$AdminUrl/api/admin/auth/sso/authorize" -Method Get -TimeoutSec 10
    } catch {
        Write-Host "WARN /sso/authorize unavailable, fallback /oidc/authorize: $_" -ForegroundColor DarkYellow
        $auth = Invoke-RestMethod -Uri "$AdminUrl/api/admin/auth/oidc/authorize" -Method Get -TimeoutSec 10
    }
    $authData = $auth.data
    if (-not $authData) { $authData = $auth }
    if ($authData.authorizationUrl -and $authData.state) {
        Write-Host "OK state=$($authData.state)"
    } else {
        throw "authorize response incomplete"
    }
}

Write-Host "`n[4] Legacy OIDC config alias" -ForegroundColor Yellow
$oidcCfg = Invoke-RestMethod -Uri "$AdminUrl/api/admin/auth/oidc/config" -Method Get -TimeoutSec 10
$oidcData = $oidcCfg.data
if (-not $oidcData) { $oidcData = $oidcCfg }
Write-Host "OK provider=$($oidcData.provider) enabled=$($oidcData.enabled)"

Write-Host "`n== Done ==" -ForegroundColor Green
Write-Host "Manual: browser login -> SSO -> verify llm_admin_user.sso_subject"
