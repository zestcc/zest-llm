# 本地完整版验证：mvn test + Admin 全量 API 验收
param(
    [string]$AdminUrl = "http://127.0.0.1:8088",
    [switch]$SkipMaven,
    [switch]$SkipAcceptance
)

$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent

if (-not $SkipMaven) {
    Write-Host "== mvn test ==" -ForegroundColor Cyan
    Push-Location $Root
    mvn -B test
    if ($LASTEXITCODE -ne 0) { throw "mvn test failed with exit $LASTEXITCODE" }
    Pop-Location
    Write-Host "mvn test: OK" -ForegroundColor Green
}

if (-not $SkipAcceptance) {
    Write-Host "== full-acceptance.ps1 ==" -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot "full-acceptance.ps1") -AdminUrl $AdminUrl
    if ($LASTEXITCODE -ne 0) { throw "full-acceptance failed with exit $LASTEXITCODE" }
}

Write-Host "All local verification passed." -ForegroundColor Green
