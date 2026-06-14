# 本地启动 ZestStory Admin，自动对齐 ZestLLM 端口；8080 被占时用 8095
param(
    [switch]$StopOnly,
    [string]$AdminUrl = "",
    [int]$ZestoryPort = 0,
    [string]$ZestoryDir = ""
)
$ErrorActionPreference = "Stop"
$ZestLlmRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $ZestoryDir) { $ZestoryDir = Join-Path (Split-Path $ZestLlmRoot -Parent) "zestory" }
$LogDir = Join-Path $ZestLlmRoot "deploy\logs"
$PidDir = Join-Path $LogDir "pids"
$PidFile = Join-Path $PidDir "zestory-local.pid"
$ZestoryPortFile = Join-Path $PidDir "zestory-local.port"
$LogFile = Join-Path $LogDir "zestory-local.log"
$ErrFile = Join-Path $LogDir "zestory-local.err.log"
$AdminPortFile = Join-Path $PidDir "admin-local.port"

function Test-PortListening([int]$Port) {
    return $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1)
}

function Test-ZestoryReady([int]$Port, [string]$CpUrl) {
    try {
        $st = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/api/integrations/status" -TimeoutSec 3
        if ($st.data.llm.zestllmReady -ne $true) { return $false }
        $cp = $st.data.llm.zestllmControlPlane
        if ($CpUrl -and $cp -and ($cp -ne $CpUrl)) { return $false }
        return $true
    } catch { return $false }
}

function Test-ZestoryEndpoint([int]$Port) {
    try {
        $st = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/api/integrations/status" -TimeoutSec 2
        return ($null -ne $st.data) -and ($null -ne $st.data.llm)
    } catch { return $false }
}

function Resolve-ZestoryPort([int]$Requested) {
    if ($Requested -gt 0) { return $Requested }
    foreach ($p in 8080, 8095, 8096, 8097) {
        if (Test-ZestoryReady $p $AdminUrl) { return $p }
    }
    # 优先 8095+，避免 8080 被其它服务占用
    foreach ($p in 8095, 8096, 8097, 8080) {
        if (-not (Test-PortListening $p)) { return $p }
        if (-not (Test-ZestoryEndpoint $p)) {
            Write-Host "Port $p in use (not ZestStory) — skip" -ForegroundColor DarkYellow
        }
    }
    throw "No free port for ZestStory in 8095-8097,8080"
}

function Stop-ZestflowPorts {
    foreach ($port in 20550, 20551) {
        Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | ForEach-Object {
            Write-Host "Stopping process on :$port PID $($_.OwningProcess)" -ForegroundColor Yellow
            Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
        }
    }
}

if ($StopOnly) {
    Stop-ZestflowPorts
    if (Test-Path $PidFile) {
        $raw = Get-Content $PidFile -ErrorAction SilentlyContinue
        if ($raw -match '^\d+$') { Stop-Process -Id ([int]$raw) -Force -ErrorAction SilentlyContinue }
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    }
    Remove-Item $ZestoryPortFile -Force -ErrorAction SilentlyContinue
    Write-Host "ZestStory stopped."
    exit 0
}

if (-not (Test-Path (Join-Path $ZestoryDir "pom.xml"))) { throw "ZestStory not found at $ZestoryDir" }

if (-not $AdminUrl) {
    if (Test-Path $AdminPortFile) {
        $AdminUrl = "http://127.0.0.1:$((Get-Content $AdminPortFile -Raw).Trim())"
    } else { $AdminUrl = "http://127.0.0.1:8088" }
}

$ZestoryPort = Resolve-ZestoryPort $ZestoryPort
$ZestoryPort | Set-Content $ZestoryPortFile -Encoding ascii

if (Test-ZestoryReady $ZestoryPort $AdminUrl) {
    Write-Host "ZestStory :$ZestoryPort already OK zestllmReady=true CP=$AdminUrl"
    exit 0
}

$env:ZESTLLM_CONTROL_PLANE_URL = $AdminUrl
$env:SERVER_PORT = "$ZestoryPort"
Write-Host "Starting ZestStory :$ZestoryPort -> ZestLLM $AdminUrl"

New-Item -ItemType Directory -Force -Path $PidDir | Out-Null
Stop-ZestflowPorts
if (Test-Path $PidFile) {
    $raw = Get-Content $PidFile -ErrorAction SilentlyContinue
    if ($raw -match '^\d+$') { Stop-Process -Id ([int]$raw) -Force -ErrorAction SilentlyContinue }
}

$proc = Start-Process -FilePath "mvn" `
    -ArgumentList @("-pl", "zestory-admin", "spring-boot:run", "-q", "-Dspring-boot.run.jvmArguments=-Dserver.port=$ZestoryPort") `
    -WorkingDirectory $ZestoryDir `
    -RedirectStandardOutput $LogFile `
    -RedirectStandardError $ErrFile `
    -PassThru -WindowStyle Hidden
$proc.Id | Set-Content $PidFile

for ($i = 1; $i -le 40; $i++) {
    if (Test-ZestoryReady $ZestoryPort $AdminUrl) {
        Write-Host "ZestStory :$ZestoryPort OK zestllmReady=true"
        exit 0
    }
    Start-Sleep -Seconds 2
}
Get-Content $ErrFile -Tail 15 -ErrorAction SilentlyContinue
Get-Content $LogFile -Tail 15 -ErrorAction SilentlyContinue
throw "ZestStory failed — see $LogFile"
