# 本地开发：启动 MySQL（Docker）并提示 Admin / UI 命令
$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$Deploy = Join-Path $Root "deploy"

Write-Host "== 启动 MySQL (docker compose) ==" -ForegroundColor Cyan
Push-Location $Deploy
docker compose up -d mysql
Pop-Location

Write-Host ""
Write-Host "下一步：" -ForegroundColor Green
Write-Host "1. 复制 zest-llm-admin/src/main/resources/application-local.example.yml -> application-local.yml"
Write-Host "2. IDEA 运行 ZestLlmAdminApplication，Active profiles: local"
Write-Host "3. cd zest-llm-admin-ui && npm run dev  （:5174，代理 :8088）"
Write-Host "4. Flyway 需执行至 V17（含 agent probe + archive run log）"
