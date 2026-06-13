# Windows 本地启动 MCP WireMock（无需 Docker）
# 用法: powershell -File deploy/scripts/start-mcp-mock-local.ps1 [-StopOnly]
param(
    [switch]$StopOnly
)

$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$ToolsDir = Join-Path $Root "deploy\tools"
$WiremockJar = Join-Path $ToolsDir "wiremock-standalone-3.3.1.jar"
$WiremockUrl = "https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/3.3.1/wiremock-standalone-3.3.1.jar"
$MockRoot = Join-Path $Root "deploy\mcp-mock"
$LogDir = Join-Path $Root "deploy\logs"
$LogFile = Join-Path $LogDir "mcp-mock-local.log"
$LogErr = Join-Path $LogDir "mcp-mock-local.err.log"
$PidFile = Join-Path $LogDir "pids\mcp-mock-local.pid"
$Port = 9090
$HealthUrl = "http://127.0.0.1:${Port}/__admin/health"

function Stop-McpMock {
    if (Test-Path $PidFile) {
        $raw = Get-Content $PidFile -ErrorAction SilentlyContinue
        if ($raw -match '^\d+$') {
            $procId = [int]$raw
            $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
            if ($proc) {
                Write-Host "Stopping MCP mock PID $procId" -ForegroundColor Yellow
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

function Test-McpHealth {
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
    Stop-McpMock
    Write-Host "MCP mock stopped." -ForegroundColor Green
    exit 0
}

New-Item -ItemType Directory -Force -Path $LogDir, (Join-Path $LogDir "pids") | Out-Null
Ensure-WiremockJar

Stop-McpMock
Start-Sleep -Seconds 1

if (Test-McpHealth) {
    Write-Host "MCP mock already up: http://127.0.0.1:${Port}/mcp" -ForegroundColor Green
    exit 0
}

Write-Host "== Starting MCP WireMock on :$Port ==" -ForegroundColor Cyan
Write-Host "Root: $MockRoot"

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
        throw "MCP mock exited early (code $($proc.ExitCode))"
    }
    if (Test-McpHealth) {
        $ok = $true
        break
    }
    Start-Sleep -Seconds 1
}

if (-not $ok) {
    Get-Content $LogErr -Tail 40 -ErrorAction SilentlyContinue
    throw "MCP mock health timeout — see $LogErr"
}

Write-Host "MCP mock is up: http://127.0.0.1:${Port}/mcp" -ForegroundColor Green
Write-Host "Stop: powershell -File deploy/scripts/start-mcp-mock-local.ps1 -StopOnly"
