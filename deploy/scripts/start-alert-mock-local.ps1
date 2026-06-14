# Windows 本地启动 Integration Webhook WireMock（无需 Docker）
# 用法: powershell -File deploy/scripts/start-alert-mock-local.ps1 [-StopOnly]
param(
    [switch]$StopOnly
)

$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$ToolsDir = Join-Path $Root "deploy\tools"
$WiremockJar = Join-Path $ToolsDir "wiremock-standalone-3.3.1.jar"
$WiremockUrl = "https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/3.3.1/wiremock-standalone-3.3.1.jar"
$MockRoot = Join-Path $Root "deploy\alert-mock"
$LogDir = Join-Path $Root "deploy\logs"
$LogFile = Join-Path $LogDir "alert-mock-local.log"
$LogErr = Join-Path $LogDir "alert-mock-local.err.log"
$PidFile = Join-Path $LogDir "pids\alert-mock-local.pid"
$Port = 8090
$HealthUrl = "http://127.0.0.1:${Port}/__admin/health"
$WebhookUrl = "http://127.0.0.1:${Port}/webhook"

function Stop-AlertMock {
    if (Test-Path $PidFile) {
        $raw = Get-Content $PidFile -ErrorAction SilentlyContinue
        if ($raw -match '^\d+$') {
            $procId = [int]$raw
            $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
            if ($proc) {
                Write-Host "Stopping alert mock PID $procId" -ForegroundColor Yellow
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            }
        }
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    }
    try {
        $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        foreach ($c in $conns) {
            $pid = $c.OwningProcess
            if ($pid -and $pid -gt 0) {
                Write-Host "Freeing port $Port (PID $pid)" -ForegroundColor Yellow
                Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
            }
        }
    } catch { }
}

function Test-AlertHealth {
    try {
        $r = Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing -TimeoutSec 5
        return $r.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Ensure-WiremockJar {
    if (Test-Path $WiremockJar) { return }
    New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
    Write-Host "Downloading WireMock standalone (one-time)..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri $WiremockUrl -OutFile $WiremockJar -UseBasicParsing
}

if ($StopOnly) {
    Stop-AlertMock
    Write-Host "Alert mock stopped." -ForegroundColor Green
    exit 0
}

New-Item -ItemType Directory -Force -Path $LogDir, (Join-Path $LogDir "pids") | Out-Null
Ensure-WiremockJar

Stop-AlertMock
Start-Sleep -Seconds 1

if (Test-AlertHealth) {
    Write-Host "Alert mock already up: $WebhookUrl" -ForegroundColor Green
    exit 0
}

Write-Host "== Starting alert WireMock on :$Port ==" -ForegroundColor Cyan
Write-Host "Webhook endpoint: $WebhookUrl"

"" | Set-Content $LogFile -Encoding UTF8
"" | Set-Content $LogErr -Encoding UTF8

$proc = Start-Process -FilePath "java" `
    -ArgumentList @(
        "-jar", $WiremockJar,
        "--port", "$Port",
        "--root-dir", $MockRoot,
        "--global-response-templating"
    ) `
    -WorkingDirectory $MockRoot `
    -RedirectStandardOutput $LogFile `
    -RedirectStandardError $LogErr `
    -PassThru -WindowStyle Hidden
$proc.Id | Set-Content $PidFile

$ok = $false
for ($i = 1; $i -le 30; $i++) {
    if ($proc.HasExited) {
        Get-Content $LogErr -Tail 40 -ErrorAction SilentlyContinue
        throw "Alert mock exited early (code $($proc.ExitCode))"
    }
    if (Test-AlertHealth) {
        $ok = $true
        break
    }
    Start-Sleep -Seconds 1
}

if (-not $ok) {
    Get-Content $LogErr -Tail 40 -ErrorAction SilentlyContinue
    throw "Alert mock health timeout — see $LogErr"
}

Write-Host "Alert mock is up: $WebhookUrl" -ForegroundColor Green
Write-Host "Set: `$env:ZEST_INTEGRATION_WEBHOOK_URL='$WebhookUrl'"
Write-Host "Stop: powershell -File deploy/scripts/start-alert-mock-local.ps1 -StopOnly"
