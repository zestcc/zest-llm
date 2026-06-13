# Windows 本地启动 LiteLLM（无需 Docker）
# 编码：请用 UTF-8 with BOM 保存本文件，避免中文注释与下一行代码被 PowerShell 误解析。
# 用法: powershell -File deploy/scripts/start-litellm-local.ps1 [-StopOnly] [-RealModels]
param(
    [switch]$StopOnly,
    [switch]$RealModels
)

$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$ConfigLocal = Join-Path $Root "deploy\litellm\config-local.yaml"
$ConfigDefault = Join-Path $Root "deploy\litellm\config.yaml"
if ($RealModels) {
    $Config = $ConfigDefault
} elseif (Test-Path $ConfigLocal) {
    $Config = $ConfigLocal
} else {
    $Config = $ConfigDefault
}
$EnvFile = Join-Path $Root "deploy\litellm\.env"
$LogDir = Join-Path $Root "deploy\logs"
$LogFile = Join-Path $LogDir "litellm-local.log"
$LogErr = Join-Path $LogDir "litellm-local.err.log"
$PidFile = Join-Path $LogDir "pids\litellm-local.pid"
$HealthUrl = "http://127.0.0.1:4000/health/liveliness"

function Stop-LiteLLM {
    if (Test-Path $PidFile) {
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
    try {
        $conns = Get-NetTCPConnection -LocalPort 4000 -State Listen -ErrorAction SilentlyContinue
        foreach ($c in $conns) {
            $pid = $c.OwningProcess
            if ($pid -and $pid -gt 0) {
                Write-Host "Freeing port 4000 (PID $pid)" -ForegroundColor Yellow
                Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
            }
        }
    } catch {
        # Get-NetTCPConnection may require admin; ignore
    }
}

function Test-LiteLLMHealth {
    param([string]$Url)
    try {
        $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
        return $r.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Show-LiteLLMLogs {
    Write-Host "---- litellm-local.err.log (tail) ----" -ForegroundColor Yellow
    if (Test-Path $LogErr) { Get-Content $LogErr -Tail 40 -ErrorAction SilentlyContinue }
    Write-Host "---- litellm-local.log (tail) ----" -ForegroundColor Yellow
    if (Test-Path $LogFile) { Get-Content $LogFile -Tail 25 -ErrorAction SilentlyContinue }
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

if (-not (Test-Path $Config)) {
    throw "Config not found: $Config"
}

Stop-LiteLLM
Start-Sleep -Seconds 2

if (Test-Path $EnvFile) {
    Write-Host "Loading env from deploy/litellm/.env" -ForegroundColor Cyan
    Get-Content $EnvFile -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        if ($line -match '^([^=]+)=(.*)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim().Trim('"').Trim("'")
            Set-Item -Path "env:$name" -Value $value
        }
    }
}

# Already running?
if (Test-LiteLLMHealth $HealthUrl) {
    Write-Host "LiteLLM already listening on :4000" -ForegroundColor Green
    Write-Host "Health: $HealthUrl"
    exit 0
}

Write-Host "== Starting LiteLLM on :4000 ==" -ForegroundColor Cyan
Write-Host "Config: $Config"
$usingMock = $Config -like "*config-local*"
if (-not $usingMock) {
    if (-not $env:DEEPSEEK_API_KEY) {
        Write-Host "WARN: DEEPSEEK_API_KEY not set — deepseek-* models will fail at invoke time." -ForegroundColor Yellow
    }
    if (-not $env:ALI_API_KEY) {
        Write-Host "WARN: ALI_API_KEY not set — ali-* MaaS models will fail at invoke time." -ForegroundColor Yellow
    }
} else {
    Write-Host "Mode: mock smoke (gpt-4o-mini / gpt-3.5-turbo). Use -RealModels for config.yaml upstream." -ForegroundColor Cyan
}
Write-Host "LiteLLM master_key (for ZestLLM): sk-zest-llm-demo"
Write-Host ""

$env:PYTHONUTF8 = "1"
$env:PYTHONIOENCODING = "utf-8"

# Fresh log files
"" | Set-Content $LogFile -Encoding UTF8
"" | Set-Content $LogErr -Encoding UTF8

$proc = Start-Process -FilePath "litellm" `
    -ArgumentList @("--config", $Config, "--port", "4000", "--host", "127.0.0.1") `
    -WorkingDirectory $Root `
    -RedirectStandardOutput $LogFile `
    -RedirectStandardError $LogErr `
    -PassThru -WindowStyle Hidden
$proc.Id | Set-Content $PidFile

Start-Sleep -Seconds 4

$ok = $false
for ($i = 1; $i -le 45; $i++) {
    if ($proc.HasExited) {
        Write-Host "LiteLLM process exited early (exit code $($proc.ExitCode))." -ForegroundColor Red
        Show-LiteLLMLogs
        throw "LiteLLM failed to start — see deploy/logs/litellm-local.err.log"
    }
    if (Test-LiteLLMHealth $HealthUrl) {
        $ok = $true
        break
    }
    if (Test-LiteLLMHealth "http://127.0.0.1:4000/health") {
        $ok = $true
        break
    }
    Write-Host "  waiting for :4000 ... ($i/45)" -ForegroundColor DarkGray
    Start-Sleep -Seconds 2
}

if (-not $ok) {
    Show-LiteLLMLogs
    throw "LiteLLM health check timeout — see deploy/logs/litellm-local.err.log"
}

Write-Host "LiteLLM is up: http://127.0.0.1:4000" -ForegroundColor Green
if ($usingMock) {
    Write-Host "Models (mock): gpt-4o-mini, gpt-3.5-turbo — no API Key required for smoke tests"
} else {
    Write-Host "Models: deepseek-v4-*, ali-deepseek-v4-*, ali-qwen3.7-max (see deploy/litellm/.env)"
}
Write-Host "Log:  $LogFile"
Write-Host "Stop: powershell -File deploy/scripts/start-litellm-local.ps1 -StopOnly"
