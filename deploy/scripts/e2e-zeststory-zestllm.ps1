# ZestStory × ZestLLM 联调 + RAG 冒烟（Windows）
#
# 用例：E2E-01 zestStoryInvoke · RAG-01 zestStoryRag · E2E-02 zestory zestllmReady · WH-01 webhook smoke · DOCKER-01（Linux CI）
#
# 前置（ZestLLM 侧，-SkipStart 时须已运行）：
#   powershell -File deploy/scripts/start-kb-mock-local.ps1
#   $env:ZEST_LLM_ADAPTERS_KNOWLEDGE_RETRIEVAL='http-knowledge'
#   $env:ZEST_LLM_HTTP_KNOWLEDGE_BASE_URL='http://127.0.0.1:8092'
#   powershell -File deploy/scripts/start-local-full.ps1 -WithLiteLLM -SkipBuild
#
# 前置（ZestStory 侧，E2E-02/WH-01 需要 :8080）：
#   powershell -File deploy/scripts/start-zestory-local.ps1   # 自动读 admin-local.port
#   或：$env:ZESTLLM_CONTROL_PLANE_URL='http://127.0.0.1:8089'; mvn -pl zestory-admin spring-boot:run
#
# 运行：
#   powershell -File deploy/scripts/e2e-zeststory-zestllm.ps1 -WithZestStory
#   powershell -File deploy/scripts/e2e-zeststory-zestllm.ps1 -SkipStart -WithZestStory
#
param(
    [switch]$SkipStart,
    [switch]$WithZestStory,
    [string]$AdminUrl = ""
)
$ErrorActionPreference = "Stop"
$Pass = 0; $Fail = 0; $Skip = 0
function Write-E2E([string]$Id, [bool]$Ok, [string]$Msg) {
    if ($Ok) { Write-Host "PASS $Id $Msg" -ForegroundColor Green; $script:Pass++ }
    else { Write-Host "FAIL $Id $Msg" -ForegroundColor Red; $script:Fail++ }
}

$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$PortFile = Join-Path $Root "deploy\logs\pids\admin-local.port"
$ZestoryPortFile = Join-Path $Root "deploy\logs\pids\zestory-local.port"
if (-not $AdminUrl) {
    if (Test-Path $PortFile) {
        $AdminUrl = "http://127.0.0.1:$((Get-Content $PortFile -Raw).Trim())"
    } else {
        $AdminUrl = "http://127.0.0.1:8088"
    }
}
$ZestoryBase = "http://127.0.0.1:8080"
if (Test-Path $ZestoryPortFile) {
    $ZestoryBase = "http://127.0.0.1:$((Get-Content $ZestoryPortFile -Raw).Trim())"
}
Write-Host "AdminUrl=$AdminUrl ZestoryUrl=$ZestoryBase"

if (-not $SkipStart) {
    & (Join-Path $PSScriptRoot "start-local-full.ps1") -StopOnly | Out-Null
    & (Join-Path $PSScriptRoot "start-kb-mock-local.ps1")
    $env:ZEST_LLM_ADAPTERS_KNOWLEDGE_RETRIEVAL = "http-knowledge"
    $env:ZEST_LLM_HTTP_KNOWLEDGE_BASE_URL = "http://127.0.0.1:8092"
    & (Join-Path $PSScriptRoot "start-local-full.ps1") -WithLiteLLM -SkipBuild
    Start-Sleep -Seconds 10
}

if ($WithZestStory) {
    & (Join-Path $PSScriptRoot "start-zestory-local.ps1") -AdminUrl $AdminUrl
}

$token = "zeststory-runtime-dev-token"
$h = @{ Authorization = "Bearer $token" }

try {
    $body = (@{
        appKey = "zeststory"; code = "zestStoryInvoke"
        inputs = @{ systemPrompt = "novel assistant"; userMessage = "write one opening line"; taskType = "invoke" }
    } | ConvertTo-Json -Compress -Depth 5)
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
    $r = Invoke-RestMethod -Uri "$AdminUrl/v1/llm/invoke" -Method POST -Headers $h -Body $bytes -ContentType "application/json; charset=utf-8" -TimeoutSec 120
    $ans = $r.output.answer
    $ansPreview = if ($ans) { $ans.Substring(0, [Math]::Min(40, $ans.Length)) } else { "(empty)" }
    Write-E2E "E2E-01" ($r.status -eq "SUCCESS" -and $ans) "zestStoryInvoke trace=$($r.traceId) ans=$ansPreview"
} catch { Write-E2E "E2E-01" $false $_.Exception.Message }

try {
    $body = (@{
        appKey = "zeststory"; code = "zestStoryRag"
        inputs = @{ systemPrompt = "novel assistant"; userMessage = "continue from lore"; taskType = "rag" }
    } | ConvertTo-Json -Compress -Depth 5)
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
    $r = Invoke-RestMethod -Uri "$AdminUrl/v1/llm/invoke" -Method POST -Headers $h -Body $bytes -ContentType "application/json; charset=utf-8" -TimeoutSec 120
    Write-E2E "RAG-01" ($r.status -eq "SUCCESS") "zestStoryRag trace=$($r.traceId)"
} catch { Write-E2E "RAG-01" $false $_.Exception.Message }

try {
    $st = Invoke-RestMethod -Uri "$ZestoryBase/api/integrations/status" -TimeoutSec 5
    Write-E2E "E2E-02" ($st.data.llm.zestllmReady -eq $true) "zestory zestllmReady"
    try {
        $whBody = (@{
            event = "PROFILE_PUBLISH_SUCCESS"; taskCode = "zestStoryInvoke"; version = "v1"
            success = $true; message = "e2e smoke"
        } | ConvertTo-Json -Compress)
        Invoke-RestMethod -Uri "$ZestoryBase/api/integrations/zestllm/webhook" -Method POST `
            -Body $whBody -ContentType "application/json" -TimeoutSec 5 | Out-Null
        $st2 = Invoke-RestMethod -Uri "$ZestoryBase/api/integrations/status" -TimeoutSec 5
        Write-E2E "WH-01" ($st2.data.llm.lastWebhook.available -eq $true) "zestory webhook receiver"
    } catch {
        Write-E2E "WH-01" $false $_.Exception.Message
    }
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
