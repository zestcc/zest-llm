# 构建 Admin UI 并输出到 Spring Boot static 目录
param(
    [switch]$SkipNpmCi
)

$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$Ui = Join-Path $Root "zest-llm-admin-ui"
$Static = Join-Path $Root "zest-llm-admin\src\main\resources\static"

Push-Location $Ui
try {
    if (-not $SkipNpmCi -and -not (Test-Path "node_modules")) {
        Write-Host "== npm ci ==" -ForegroundColor Cyan
        npm ci
    }
    Write-Host "== npm run build ==" -ForegroundColor Cyan
    npm run build
} finally {
    Pop-Location
}

Write-Host "== UI built into static (vite outDir) ==" -ForegroundColor Green
if (-not (Test-Path (Join-Path $Static "index.html"))) {
    throw "static/index.html missing after build — check vite.config.ts outDir"
}
Write-Host "Done. Restart Admin on :8088 to serve embedded UI."
