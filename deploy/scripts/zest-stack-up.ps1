# Zest Stack 分层一键部署
# 用法: .\zest-stack-up.ps1 -Tier small|medium|large
param(
    [ValidateSet('small', 'medium', 'large')]
    [string]$Tier = 'small'
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

Write-Host "== Zest Stack Tier: $Tier ==" -ForegroundColor Cyan

docker compose up -d mysql
Start-Sleep -Seconds 8

switch ($Tier) {
    'small' {
        docker compose up -d --build
    }
    'medium' {
        docker compose `
            -f docker-compose.yml `
            -f docker-compose.observability.yml `
            -f docker-compose.plan-a.yml `
            --profile observability `
            up -d --build
    }
    'large' {
        docker compose `
            -f docker-compose.yml `
            -f docker-compose.observability.yml `
            -f docker-compose.plan-a.yml `
            -f docker-compose.kafka.yml `
            -f docker-compose.integration.yml `
            --profile observability `
            --profile kafka `
            --profile integration `
            up -d --build
    }
}

Write-Host ""
Write-Host "Zest Stack ($Tier) started." -ForegroundColor Green
Write-Host "  Admin+UI  http://localhost:8088  (admin/admin123)"
Write-Host "  Demo      http://localhost:8081"
if ($Tier -ne 'small') {
    Write-Host "  Langfuse  http://localhost:3000"
}
if ($Tier -eq 'large') {
    Write-Host "  Dify API  http://localhost:5001"
    Write-Host "  RAGFlow   http://localhost:9380"
}
Write-Host ""
Write-Host "Acceptance: ..\scripts\full-acceptance.ps1"
Write-Host "Stress:     ..\scripts\stress-test-prepare.ps1 -Concurrency 50"
