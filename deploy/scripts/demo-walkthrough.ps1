# 15 分钟 Demo 自动化（冒烟）
param([string]$AdminUrl = "http://localhost:8088")

$ErrorActionPreference = "Stop"
Write-Host "== ZestLLM Demo Walkthrough ==" -ForegroundColor Cyan

$login = Invoke-RestMethod -Uri "$AdminUrl/api/admin/auth/login" -Method POST `
  -Body '{"username":"admin","password":"admin123"}' -ContentType "application/json"
$token = $login.data.token; if (-not $token) { $token = $login.token }

function AdminGet($path) {
  Invoke-RestMethod -Uri "$AdminUrl$path" -Headers @{ Authorization = "Bearer $token" }
}

Write-Host "[1/6] Capability stack..."
AdminGet "/api/admin/capability-stack" | Out-Null

Write-Host "[2/6] Scenario templates..."
AdminGet "/api/admin/scenario-templates" | Out-Null

Write-Host "[3/6] Runtime prepare..."
$prep = Invoke-RestMethod -Uri "$AdminUrl/v1/llm/prepare" -Method POST `
  -Body '{"appKey":"order-service","code":"aiChat","inputs":{"question":"demo"}}' `
  -ContentType "application/json" -Headers @{ Authorization = "Bearer demo-token-123" }
if (-not $prep.traceId -and $prep.data.traceId) { $prep.traceId = $prep.data.traceId }

Write-Host "[4/6] Publish preview..."
AdminGet "/api/admin/agent-profiles/aiChat/versions/v1/publish-preview" | Out-Null

Write-Host "[5/6] App overview..."
AdminGet "/api/admin/apps/overview" | Out-Null

Write-Host "[6/6] AI job overview..."
AdminGet "/api/admin/ai-jobs/overview" | Out-Null

Write-Host "PASS demo walkthrough traceId=$($prep.traceId)" -ForegroundColor Green
