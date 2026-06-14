# 验证内嵌 Admin UI（:8088）与 Integration Webhook 真投递
# 前置: start-local-full.ps1 -EmbedUi -WithAlertMock -WithLiteLLM
param(
    [string]$AdminUrl = "http://127.0.0.1:8088",
    [string]$TaskCode = "aiChat",
    [string]$PublishVersion = "v1"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$ReportDir = Join-Path $Root "deploy\test-reports"
$Pass = 0
$Fail = 0

function Write-Result([string]$Id, [bool]$Ok, [string]$Detail) {
    if ($Ok) {
        Write-Host "PASS $Id $Detail" -ForegroundColor Green
        $script:Pass++
    } else {
        Write-Host "FAIL $Id $Detail" -ForegroundColor Red
        $script:Fail++
    }
}

function Get-AdminToken {
    $body = '{"username":"admin","password":"admin123"}'
    $resp = Invoke-RestMethod -Uri "$AdminUrl/api/admin/auth/login" -Method POST -Body $body -ContentType "application/json" -TimeoutSec 15
    return $resp.data.token
}

Write-Host "=== Embedded UI + Webhook smoke ===" -ForegroundColor Cyan

try {
    $index = Invoke-WebRequest -Uri "$AdminUrl/" -UseBasicParsing -TimeoutSec 10
    $html = $index.Content
    $hasAssets = $html -match '/assets/'
    $hasRoot = $html -match 'id="app"'
    Write-Result "UI-01" ($index.StatusCode -eq 200 -and $hasAssets -and $hasRoot) "embedded index.html + assets"
} catch {
    Write-Result "UI-01" $false $_.Exception.Message
}

try {
    $spa = Invoke-WebRequest -Uri "$AdminUrl/integration" -UseBasicParsing -TimeoutSec 10
    $spaOk = ($spa.StatusCode -eq 200) -and ($spa.Content -match 'id="app"')
    Write-Result "UI-02" $spaOk "/integration SPA forward"
} catch {
    Write-Result "UI-02" $false $_.Exception.Message
}

try {
    $assetsDir = Join-Path $Root "zest-llm-admin\src\main\resources\static\assets"
    $assetCount = (Get-ChildItem $assetsDir -File -ErrorAction SilentlyContinue | Measure-Object).Count
    Write-Result "UI-03" ($assetCount -gt 0) "static/assets files=$assetCount"
} catch {
    Write-Result "UI-03" $false $_.Exception.Message
}

$token = $null
try {
    $token = Get-AdminToken
    Write-Result "WH-01" ($null -ne $token) "admin login"
} catch {
    Write-Result "WH-01" $false $_.Exception.Message
}

if ($token) {
    $headers = @{ Authorization = "Bearer $token" }
    $publishOk = $false
    try {
        $preview = Invoke-RestMethod -Uri "$AdminUrl/api/admin/agent-profiles/$TaskCode/versions/$PublishVersion/publish-preview" -Headers $headers -TimeoutSec 30
        if (-not $preview.data.publishAllowed) {
            Write-Host "WARN publish-preview blocked: $($preview.data.message)" -ForegroundColor Yellow
        }
        Invoke-RestMethod -Uri "$AdminUrl/api/admin/agent-profiles/$TaskCode/publish" -Method POST `
            -Headers $headers -Body "{`"version`":`"$PublishVersion`",`"operator`":`"webhook-smoke`"}" `
            -ContentType "application/json" -TimeoutSec 60 | Out-Null
        $publishOk = $true
        Write-Result "WH-02" $true "publish $TaskCode@$PublishVersion"
    } catch {
        Write-Result "WH-02" $false "publish: $($_.Exception.Message)"
    }

    Start-Sleep -Seconds 2

    try {
        $del = Invoke-RestMethod -Uri "$AdminUrl/api/admin/integration/webhook/deliveries?page=1&size=20" -Headers $headers -TimeoutSec 15
        $records = $del.data.records
        if (-not $records) { $records = @() }
        $sent = @($records | Where-Object { $_.status -eq 'SENT' }).Count -gt 0
        $successEvent = @($records | Where-Object { $_.eventType -eq 'PROFILE_PUBLISH_SUCCESS' }).Count -gt 0
        $failedLogged = @($records | Where-Object { $_.eventType -eq 'PROFILE_PUBLISH_FAILED' }).Count -gt 0
        $wh03Ok = ($publishOk -and $sent -and $successEvent) -or ((-not $publishOk) -and $failedLogged -and ($records.Count -gt 0))
        Write-Result "WH-03" $wh03Ok "webhook delivery persisted (records=$($records.Count))"
    } catch {
        Write-Result "WH-03" $false "deliveries: $($_.Exception.Message)"
    }

    try {
        $reqs = Invoke-RestMethod -Uri "http://127.0.0.1:8090/__admin/requests" -TimeoutSec 10
        $reqJson = ($reqs | ConvertTo-Json -Depth 6 -Compress)
        $received = $reqJson -match '/webhook'
        Write-Result "WH-04" $received "alert-mock received POST /webhook"
    } catch {
        Write-Result "WH-04" $false "alert-mock requests: $($_.Exception.Message)"
    }
}

New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$report = Join-Path $ReportDir "embedded-webhook-smoke-$stamp.txt"
@"
=== Embedded UI + Webhook Smoke $stamp ===
AdminUrl=$AdminUrl
PASSED=$Pass FAILED=$Fail
"@ | Set-Content $report -Encoding UTF8

Write-Host ""
Write-Host "SUMMARY: $Pass PASS / $Fail FAIL" -ForegroundColor $(if ($Fail -eq 0) { "Green" } else { "Red" })
Write-Host "Report: $report"

if ($Fail -gt 0) { exit 1 }
