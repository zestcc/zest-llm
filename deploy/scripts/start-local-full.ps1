# ZestLLM 本地完整版一键启动（Windows，无 Docker 友好）
# 编码：请用 UTF-8 with BOM 保存本文件，避免中文注释与下一行代码被 PowerShell 误解析。
# 用法:
#   powershell -File deploy/scripts/start-local-full.ps1 -WithDemo -WithLiteLLM
#   powershell -File deploy/scripts/start-local-full.ps1 [-AdminPort 8089] [-WithLiteLLM] ...
# 生产形态（内嵌 UI + Webhook mock）:
#   powershell -File deploy/scripts/start-local-full.ps1 -EmbedUi -WithAlertMock
# 推荐一键（Demo + LiteLLM + Admin + UI dev）:
#   powershell -File deploy/scripts/start-local-full.ps1 -WithDemo -WithLiteLLM
param(
    [switch]$WithLiteLLM,
    [switch]$WithDemo,
    [switch]$WithMcpMock,
    [switch]$WithAlertMock,
    [switch]$EmbedUi,
    [switch]$SkipBuild,
    [switch]$StopOnly,
    [int]$AdminPort = 0
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
$PidDir = Join-Path $LogDir "pids"
$AdminLog = Join-Path $LogDir "admin-local.log"
$AdminErrLog = Join-Path $LogDir "admin-local.err.log"
$UiLog = Join-Path $LogDir "admin-ui-dev.log"
$AdminPidFile = Join-Path $PidDir "admin-local.pid"
$AdminPortFile = Join-Path $PidDir "admin-local.port"
$UiPidFile = Join-Path $PidDir "admin-ui-dev.pid"

function Test-PortListening([int]$Port) {
    return $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1)
}

function Resolve-AdminPort([int]$Requested) {
    if ($Requested -gt 0) { return $Requested }
    if (-not (Test-PortListening 8088)) { return 8088 }
    Write-Host "Port 8088 busy — trying alternate ports" -ForegroundColor Yellow
    foreach ($p in 8089, 8092, 8093, 8094) {
        if (-not (Test-PortListening $p)) { return $p }
    }
    throw "No free port in 8088,8089,8092-8094 for Admin (use -AdminPort or free 8088)"
}

function Stop-FromPidFile([string]$PidFile, [string]$Label) {
    if (-not (Test-Path $PidFile)) { return }
    $raw = Get-Content $PidFile -ErrorAction SilentlyContinue
    if ($raw -match '^\d+$') {
        $procId = [int]$raw
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "Stopping $Label PID $procId" -ForegroundColor Yellow
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        }
    }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
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

function Test-HttpBodyMatches([string]$Url, [string]$Pattern) {
    try {
        $html = (Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3).Content
        return $html -match $Pattern
    } catch {
        return $false
    }
}

function Wait-AdminUiDev([string]$Url, [int]$Retries = 60) {
    for ($i = 1; $i -le $Retries; $i++) {
        if (Test-HttpBodyMatches $Url 'ZestLLM Admin') { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Warn-IfWrongAppOnPort([int]$Port, [string]$Label) {
    if (-not (Test-PortListening $Port)) { return }
    $url = "http://localhost:$Port/"
    if (Test-HttpBodyMatches $url 'vitepress') {
        Write-Host "WARN: Port $Port serves VitePress (zest/website), not $Label. Stop it or Admin UI dev will fail (strictPort)." -ForegroundColor Yellow
    } elseif (-not (Test-HttpBodyMatches $url 'ZestLLM Admin')) {
        Write-Host "WARN: Port $Port is in use by another app (not $Label)." -ForegroundColor Yellow
    }
}

if ($StopOnly) {
    Stop-FromPidFile $AdminPidFile "Admin"
    Stop-FromPidFile $UiPidFile "Admin UI dev"
    Remove-Item $AdminPortFile -Force -ErrorAction SilentlyContinue
    & (Join-Path $PSScriptRoot "start-litellm-local.ps1") -StopOnly
    & (Join-Path $PSScriptRoot "start-demo-local.ps1") -StopOnly
    & (Join-Path $PSScriptRoot "start-mcp-mock-local.ps1") -StopOnly
    & (Join-Path $PSScriptRoot "start-alert-mock-local.ps1") -StopOnly
    Write-Host "Stopped local stack (PID files)." -ForegroundColor Green
    exit 0
}

New-Item -ItemType Directory -Force -Path $LogDir, $PidDir | Out-Null

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
    throw "MySQL :3306 not reachable. Start MySQL 8 locally (no Docker required)."
}

if ($WithLiteLLM) {
    Write-Host "== Starting LiteLLM via pip (config-local mock) ==" -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot "start-litellm-local.ps1")
}

if ($WithMcpMock) {
    & (Join-Path $PSScriptRoot "start-mcp-mock-local.ps1")
}

if ($WithAlertMock) {
    & (Join-Path $PSScriptRoot "start-alert-mock-local.ps1")
}

# Stop Admin/Demo before rebuild to release JAR lock (mvn package fails if still running)
if (-not $SkipBuild -or $EmbedUi) {
    Write-Host "== Stopping Admin/Demo before rebuild ==" -ForegroundColor Cyan
    Stop-FromPidFile $AdminPidFile "Admin"
    Stop-FromPidFile $UiPidFile "Admin UI dev"
    if ($WithDemo) {
        & (Join-Path $PSScriptRoot "start-demo-local.ps1") -StopOnly
    }
    Start-Sleep -Seconds 1
}

if (-not $SkipBuild) {
    Write-Host "== mvn package (skip tests) ==" -ForegroundColor Cyan
    Push-Location $Root
    if ($WithDemo) {
        mvn -pl zest-llm-admin,zest-llm-demo -am package -DskipTests -q
    } else {
        mvn -pl zest-llm-admin -am package -DskipTests -q
    }
    Pop-Location
}

if ($EmbedUi) {
    & (Join-Path $PSScriptRoot "build-admin-ui.ps1")
    Push-Location $Root
    mvn -pl zest-llm-admin -am package -DskipTests -q
    Pop-Location
}

if (-not (Test-Path $Jar)) { throw "Missing jar: $Jar — run mvn package first" }

Stop-FromPidFile $AdminPidFile "Admin"
Stop-FromPidFile $UiPidFile "Admin UI dev"
Start-Sleep -Seconds 1

$AdminPort = Resolve-AdminPort $AdminPort
$AdminBase = "http://127.0.0.1:$AdminPort"
$AdminPort | Set-Content $AdminPortFile -Encoding ascii

Write-Host "== Starting Admin (:$AdminPort) ==" -ForegroundColor Cyan
if ($WithAlertMock) {
    $env:ZEST_INTEGRATION_WEBHOOK_URL = "http://127.0.0.1:8090/webhook"
    Write-Host "  ZEST_INTEGRATION_WEBHOOK_URL=$($env:ZEST_INTEGRATION_WEBHOOK_URL)" -ForegroundColor DarkGray
}
$adminProc = Start-Process -FilePath "java" `
    -ArgumentList @("-jar", $Jar, "--spring.profiles.active=local", "--server.port=$AdminPort") `
    -WorkingDirectory $AdminDir `
    -RedirectStandardOutput $AdminLog `
    -RedirectStandardError $AdminErrLog `
    -PassThru -WindowStyle Hidden
$adminProc.Id | Set-Content $AdminPidFile

if (-not (Wait-HttpOk "$AdminBase/swagger-ui.html")) {
    Get-Content $AdminLog -Tail 40
    throw "Admin failed to start — see $AdminLog"
}

if (-not $EmbedUi) {
    $UiDevPort = 5174
    Warn-IfWrongAppOnPort $UiDevPort "ZestLLM Admin UI dev"
    Write-Host "== Starting Admin UI dev (:$UiDevPort, API -> :$AdminPort) ==" -ForegroundColor Cyan
    $uiProc = Start-Process -FilePath "cmd.exe" `
        -ArgumentList @("/c", "set VITE_ADMIN_PORT=$AdminPort&& npm run dev > `"$UiLog`" 2>&1") `
        -WorkingDirectory $UiDir `
        -PassThru -WindowStyle Hidden
    $uiProc.Id | Set-Content $UiPidFile

    $uiUrl = "http://localhost:$UiDevPort/"
    if (-not (Wait-AdminUiDev $uiUrl)) {
        Get-Content $UiLog -Tail 30
        throw "Admin UI dev failed — see $UiLog (expected ZestLLM Admin on :$UiDevPort, API proxy -> :$AdminPort)"
    }
}

if ($WithDemo) {
    if (-not $WithLiteLLM) {
        Write-Host "WARN: Demo AI 调用建议同时 -WithLiteLLM" -ForegroundColor Yellow
    }
    & (Join-Path $PSScriptRoot "start-demo-local.ps1") -SkipBuild
}

Write-Host ""
Write-Host "ZestLLM local stack is up." -ForegroundColor Green
if ($EmbedUi) {
    Write-Host "  Admin (embedded UI): $AdminBase  (login admin / admin123)"
} else {
    Write-Host "  Admin UI (dev):      http://localhost:5174  (login admin / admin123)"
    Write-Host "  Admin API:           $AdminBase"
}
Write-Host "  ZestFlow CP:         http://127.0.0.1:20552"
if ($WithLiteLLM) { Write-Host "  LiteLLM:             http://127.0.0.1:4000" }
else { Write-Host '  LiteLLM:             not started (use -WithLiteLLM for mock gateway)' }
if ($WithDemo) { Write-Host '  Demo: http://127.0.0.1:8081/demo/order/methodA?orderId=1&question=hi' }
if ($WithMcpMock) { Write-Host "  MCP mock:            http://127.0.0.1:9090/mcp" }
if ($WithAlertMock) { Write-Host "  Webhook mock:        http://127.0.0.1:8090/webhook" }
Write-Host "  Logs:                $LogDir"
Write-Host ""
Write-Host "Verify embedded UI + webhook: powershell -File deploy/scripts/verify-embedded-ui-and-webhook.ps1"
Write-Host "Accept: powershell -File deploy/scripts/full-acceptance.ps1"
Write-Host "Demo:   powershell -File deploy/scripts/demo-walkthrough.ps1"
Write-Host "Stop:   powershell -File deploy/scripts/start-local-full.ps1 -StopOnly"
