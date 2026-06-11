# Windows 本地启动 LiteLLM（无需 Docker）
# 用法: powershell -File deploy/scripts/start-litellm-local.ps1 [-StopOnly]
param(
    [switch]$StopOnly
)

$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$Config = Join-Path $Root "deploy\litellm\config.yaml"
$LogDir = Join-Path $Root "deploy\logs"
$LogFile = Join-Path $LogDir "litellm-local.log"
$LogErr = Join-Path $LogDir "litellm-local.err.log"
$PidFile = Join-Path $LogDir "pids\litellm-local.pid"

function Stop-LiteLLM {
    if (-not (Test-Path $PidFile)) { return }
    $raw = Get-Content $PidFile -ErrorAction SilentlyContinue
    if ($raw -match '^\d+$') {
        $procId = [int]$raw
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "Stopping LiteLLM PID $procId" -ForegroundColor Yellow
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        }
    }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
}

if ($StopOnly) {
    Stop-LiteLLM
    Write-Host "LiteLLM stopped." -ForegroundColor Green
    exit 0
}

New-Item -ItemType Directory -Force -Path $LogDir, (Join-Path $LogDir "pids") | Out-Null

$litellm = Get-Command litellm -ErrorAction SilentlyContinue
if (-not $litellm) {
    throw "litellm CLI not found. Run: pip install `"litellm[proxy]`""
}

Stop-LiteLLM
Start-Sleep -Seconds 1

Write-Host "== Starting LiteLLM on :4000 ==" -ForegroundColor Cyan
Write-Host "Config: $Config"
Write-Host "Set model keys before invoke, e.g.:"
Write-Host '  $env:OPENAI_API_KEY = "sk-..."'
Write-Host '  $env:DEEPSEEK_API_KEY = "sk-..."'
Write-Host "LiteLLM master_key (for ZestLLM): sk-zest-llm-demo"
Write-Host ""

$proc = Start-Process -FilePath "litellm" `
    -ArgumentList @("--config", $Config, "--port", "4000") `
    -WorkingDirectory $Root `
    -RedirectStandardOutput $LogFile `
    -RedirectStandardError $LogErr `
    -PassThru -WindowStyle Hidden
$proc.Id | Set-Content $PidFile

$ok = $false
for ($i = 1; $i -le 30; $i++) {
    try {
        $r = Invoke-WebRequest -Uri "http://127.0.0.1:4000/health/liveliness" -UseBasicParsing -TimeoutSec 3
        if ($r.StatusCode -eq 200) { $ok = $true; break }
    } catch { }
    Start-Sleep -Seconds 2
}

if (-not $ok) {
    Get-Content $LogFile -Tail 30 -ErrorAction SilentlyContinue
    throw "LiteLLM failed to start — see $LogFile"
}

Write-Host "LiteLLM is up: http://127.0.0.1:4000" -ForegroundColor Green
Write-Host "Log:  $LogFile"
Write-Host "Stop: powershell -File deploy/scripts/start-litellm-local.ps1 -StopOnly"
