# ZestStory × ZestLLM 联调 + RAG 冒烟（Windows）
param([switch]$SkipStart)
$ErrorActionPreference = "Stop"
$Pass = 0; $Fail = 0; $Skip = 0
function Write-E2E([string]$Id, [bool]$Ok, [string]$Msg) {
    if ($Ok) { Write-Host "PASS $Id $Msg" -ForegroundColor Green; $script:Pass++ }
    else { Write-Host "FAIL $Id $Msg" -ForegroundColor Red; $script:Fail++ }
}

if (-not $SkipStart) {
    & (Join-Path $PSScriptRoot "start-local-full.ps1") -StopOnly | Out-Null
    & (Join-Path $PSScriptRoot "start-kb-mock-local.ps1")
    $env:ZEST_LLM_ADAPTERS_KNOWLEDGE_RETRIEVAL = "http-knowledge"
    $env:ZEST_LLM_HTTP_KNOWLEDGE_BASE_URL = "http://127.0.0.1:8091"
    & (Join-Path $PSScriptRoot "start-local-full.ps1") -WithLiteLLM -SkipBuild
    Start-Sleep -Seconds 10
}

$token = "zeststory-runtime-dev-token"
$h = @{ Authorization = "Bearer $token" }

try {
    $body = (@{
        appKey = "zeststory"; code = "zestStoryInvoke"
        inputs = @{ systemPrompt = "novel assistant"; userMessage = "write one opening line" }
    } | ConvertTo-Json -Compress -Depth 5)
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
    $r = Invoke-RestMethod -Uri "http://127.0.0.1:8088/v1/llm/invoke" -Method POST -Headers $h -Body $bytes -ContentType "application/json; charset=utf-8" -TimeoutSec 120
    $ans = $r.output.answer
    Write-E2E "E2E-01" ($r.status -eq "SUCCESS" -and $ans) "zestStoryInvoke trace=$($r.traceId) ans=$($ans.Substring(0,[Math]::Min(40,$ans.Length)))"
} catch { Write-E2E "E2E-01" $false $_.Exception.Message }

try {
    $body = (@{
        appKey = "zeststory"; code = "zestStoryRag"
        inputs = @{ systemPrompt = "novel assistant"; userMessage = "continue from lore" }
    } | ConvertTo-Json -Compress -Depth 5)
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
    $r = Invoke-RestMethod -Uri "http://127.0.0.1:8088/v1/llm/invoke" -Method POST -Headers $h -Body $bytes -ContentType "application/json; charset=utf-8" -TimeoutSec 120
    Write-E2E "RAG-01" ($r.status -eq "SUCCESS") "zestStoryRag trace=$($r.traceId)"
} catch { Write-E2E "RAG-01" $false $_.Exception.Message }

try {
    $st = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/integrations/status" -TimeoutSec 5
    Write-E2E "E2E-02" ($st.data.llm.zestllmReady -eq $true) "zestory zestllmReady"
} catch {
    Write-Host "SKIP E2E-02 zestory not running" -ForegroundColor Yellow
    $Skip++
}

if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Host "SKIP DOCKER-01 run production-acceptance.sh on Linux CI" -ForegroundColor Yellow
} else {
    Write-Host "SKIP DOCKER-01 no Docker — Linux CI only" -ForegroundColor Yellow
}
$Skip++

Write-Host "`nSUMMARY: $Pass PASS / $Fail FAIL / $Skip SKIP"
if ($Fail -gt 0) { exit 1 }
