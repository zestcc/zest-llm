# ZestLLM 本地完整版一键启动（Windows）
# 用法: powershell -File deploy/scripts/start-local-full.ps1 [-WithLiteLLM] [-EmbedUi] [-SkipBuild]
param(
    [switch]$WithLiteLLM,
    [switch]$EmbedUi,
    [switch]$SkipBuild,
    [switch]$StopOnly
)

$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$Deploy = Join-Path $Root "deploy"
$AdminDir = Join-Path $Root "zest-llm-admin"
$UiDir = Join-Path $Root "zest-llm-admin-ui"
$LocalYml = Join-Path $AdminDir "src\main\resources\application-local.yml"
$ExampleYml = Join-Path $AdminDir "src\main\resources\application-local.example.yml"
$Jar = Join-Path $AdminDir "target\zest-llm-admin-1.0.0.jar"
$LogDir = Join-Path $Root "deploy\logs"
$AdminLog = Join-Path $LogDir "admin-local.log"
$AdminErrLog = Join-Path $LogDir "admin-local.err.log"
$UiLog = Join-Path $LogDir "admin-ui-dev.log"

function Stop-PortListener([int]$Port) {
    $lines = netstat -ano | Select-String ":$Port\s"
    foreach ($line in $lines) {
        if ($line -match '\s+LISTENING\s+(\d+)\s*$') {
            $procId = [int]$Matches[1]
            if ($procId -gt 0) {
                Write-Host "Stopping PID $procId on port $Port" -ForegroundColor Yellow
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

function Wait-HttpOk([string]$Url, [int]$Retries = 60) {
    for ($i = 1; $i -le $Retries; $i++) {
        try {
            $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500) { return $true }
        } catch {
            if ($_.Exception.Response) { return $true }
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

if ($StopOnly) {
    Stop-PortListener 8088
    Stop-PortListener 5174
    Write-Host "Stopped Admin (:8088) and UI dev (:5174) if running." -ForegroundColor Green
    exit 0
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

if (-not (Test-Path $LocalYml)) {
    if (-not (Test-Path $ExampleYml)) { throw "Missing $ExampleYml" }
    Copy-Item $ExampleYml $LocalYml
    Write-Host "Created application-local.yml from example — edit MySQL password if needed." -ForegroundColor Yellow
}

$mysqlUp = $false
try {
    $tcp = Test-NetConnection -ComputerName 127.0.0.1 -Port 3306 -WarningAction SilentlyContinue
    $mysqlUp = $tcp.TcpTestSucceeded
} catch { $mysqlUp = $false }

if (-not $mysqlUp) {
    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if ($docker) {
        Write-Host "== Starting MySQL via Docker ==" -ForegroundColor Cyan
        Push-Location $Deploy
        docker compose up -d mysql
        Pop-Location
        Start-Sleep -Seconds 8
    } else {
        throw "MySQL :3306 not reachable and Docker not found. Start MySQL manually."
    }
}

if ($WithLiteLLM) {
    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if (-not $docker) { throw "-WithLiteLLM requires Docker" }
    Write-Host "== Starting LiteLLM + openai-mock ==" -ForegroundColor Cyan
    Push-Location $Deploy
    docker compose up -d openai-mock litellm
    Pop-Location
}

if (-not $SkipBuild) {
    Write-Host "== mvn package (skip tests) ==" -ForegroundColor Cyan
    Push-Location $Root
    mvn -pl zest-llm-admin -am package -DskipTests -q
    Pop-Location
}

if ($EmbedUi) {
    & (Join-Path $PSScriptRoot "build-admin-ui.ps1")
    Push-Location $Root
    mvn -pl zest-llm-admin -am package -DskipTests -q
    Pop-Location
}

if (-not (Test-Path $Jar)) { throw "Missing jar: $Jar — run mvn package first" }

Stop-PortListener 8088
Stop-PortListener 5174
Start-Sleep -Seconds 1

Write-Host "== Starting Admin (:8088) ==" -ForegroundColor Cyan
$adminProc = Start-Process -FilePath "java" `
    -ArgumentList @("-jar", $Jar, "--spring.profiles.active=local") `
    -WorkingDirectory $AdminDir `
    -RedirectStandardOutput $AdminLog `
    -RedirectStandardError $AdminErrLog `
    -PassThru -WindowStyle Hidden

if (-not (Wait-HttpOk "http://127.0.0.1:8088/swagger-ui.html")) {
    Get-Content $AdminLog -Tail 40
    throw "Admin failed to start — see $AdminLog"
}

Write-Host "== Starting Admin UI dev (:5174) ==" -ForegroundColor Cyan
$uiProc = Start-Process -FilePath "cmd.exe" `
    -ArgumentList @("/c", "npm run dev > `"$UiLog`" 2>&1") `
    -WorkingDirectory $UiDir `
    -PassThru -WindowStyle Hidden

if (-not (Wait-HttpOk "http://localhost:5174/")) {
    Get-Content $UiLog -Tail 30
    throw "Admin UI dev failed — see $UiLog"
}

Write-Host ""
Write-Host "ZestLLM local stack is up." -ForegroundColor Green
Write-Host "  Admin UI (dev):  http://localhost:5174  (login admin / admin123)"
Write-Host "  Admin API:       http://127.0.0.1:8088"
Write-Host "  ZestFlow CP:     http://127.0.0.1:20552"
if ($WithLiteLLM) { Write-Host "  LiteLLM:         http://127.0.0.1:4000 (Model Gateway UP)" }
else { Write-Host "  LiteLLM:         not started (Dashboard may show Gateway DOWN)" }
Write-Host "  Logs:            $LogDir"
Write-Host ""
Write-Host "Verify: powershell -File deploy/scripts/verify-local.ps1"
Write-Host "Stop:   powershell -File deploy/scripts/start-local-full.ps1 -StopOnly"
