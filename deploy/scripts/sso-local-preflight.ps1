# SSO 本地联调预检：Discovery + Admin config/authorize + 可选 DB 验证
# 用法: powershell -File deploy/scripts/sso-local-preflight.ps1 [-AdminUrl http://127.0.0.1:8088] [-SsoBase http://localhost:9000] [-VerifyDb]
param(
    [string]$AdminUrl = "http://127.0.0.1:8088",
    [string]$SsoBase = "http://localhost:9000",
    [switch]$VerifyDb
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path $MyInvocation.MyCommand.Path -Parent

Write-Host "== SSO local preflight ==" -ForegroundColor Cyan
& (Join-Path $scriptDir "sso-smoke.ps1") -AdminUrl $AdminUrl -SsoBase $SsoBase
if ($LASTEXITCODE -ne 0) {
    Write-Host "FAIL sso-smoke" -ForegroundColor Red
    exit 1
}

if ($VerifyDb) {
    & (Join-Path $scriptDir "sso-db-verify.ps1")
    if ($LASTEXITCODE -ne 0) {
        Write-Host "WARN DB verify — browser login may be pending" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host "PASS sso-local-preflight" -ForegroundColor Green
Write-Host "Next: deploy/scripts/sso-browser-checklist.md (browser login + sso_subject)"
exit 0
