# ZestLLM 本地完整版一键启动（Windows，无 Docker 友好）
# 用法: powershell -File deploy/scripts/start-local-full.ps1 [-WithLiteLLM] [-WithDemo] [-WithMcpMock] [-EmbedUi] [-SkipBuild] [-StopOnly]
param(
    [switch]$WithLiteLLM,
    [switch]$WithDemo,
    [switch]$WithMcpMock,
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
$PidDir = Join-Path $LogDir "pids"
$AdminLog = Join-Path $LogDir "admin-local.log"
$AdminErrLog = Join-Path $LogDir "admin-local.err.log"
$UiLog = Join-Path $LogDir "admin-ui-dev.log"
$AdminPidFile = Join-Path $PidDir "admin-local.pid"
$UiPidFile = Join-Path $PidDir "admin-ui-dev.pid"

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

if ($StopOnly) {
    Stop-FromPidFile $AdminPidFile "Admin"
    Stop-FromPidFile $UiPidFile "Admin UI dev"
    & (Join-Path $PSScriptRoot "start-litellm-local.ps1") -StopOnly
    & (Join-Path $PSScriptRoot "start-demo-local.ps1") -StopOnly
    & (Join-Path $PSScriptRoot "start-mcp-mock-local.ps1") -StopOnly
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

Write-Host "== Starting Admin (:8088) ==" -ForegroundColor Cyan
$adminProc = Start-Process -FilePath "java" `
    -ArgumentList @("-jar", $Jar, "--spring.profiles.active=local") `
    -WorkingDirectory $AdminDir `
    -RedirectStandardOutput $AdminLog `
    -RedirectStandardError $AdminErrLog `
    -PassThru -WindowStyle Hidden
$adminProc.Id | Set-Content $AdminPidFile

if (-not (Wait-HttpOk "http://127.0.0.1:8088/swagger-ui.html")) {
    Get-Content $AdminLog -Tail 40
    throw "Admin failed to start — see $AdminLog"
}

if (-not $EmbedUi) {
    Write-Host "== Starting Admin UI dev (:5174) ==" -ForegroundColor Cyan
    $uiProc = Start-Process -FilePath "cmd.exe" `
        -ArgumentList @("/c", "npm run dev > `"$UiLog`" 2>&1") `
        -WorkingDirectory $UiDir `
        -PassThru -WindowStyle Hidden
    $uiProc.Id | Set-Content $UiPidFile

    if (-not (Wait-HttpOk "http://localhost:5174/")) {
        Get-Content $UiLog -Tail 30
        throw "Admin UI dev failed — see $UiLog"
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
    Write-Host "  Admin (embedded UI): http://127.0.0.1:8088  (login admin / admin123)"
} else {
    Write-Host "  Admin UI (dev):      http://localhost:5174  (login admin / admin123)"
    Write-Host "  Admin API:           http://127.0.0.1:8088"
}
Write-Host "  ZestFlow CP:         http://127.0.0.1:20552"
if ($WithLiteLLM) { Write-Host "  LiteLLM:             http://127.0.0.1:4000" }
else { Write-Host '  LiteLLM:             not started (use -WithLiteLLM for mock gateway)' }
if ($WithDemo) { Write-Host '  Demo: http://127.0.0.1:8081/demo/order/methodA?orderId=1&question=hi' }
if ($WithMcpMock) { Write-Host "  MCP mock:            http://127.0.0.1:9090/mcp" }
Write-Host "  Logs:                $LogDir"
Write-Host ""
Write-Host "Verify: powershell -File deploy/scripts/verify-local.ps1"
Write-Host "Demo:   powershell -File deploy/scripts/demo-walkthrough.ps1"
Write-Host "Stop:   powershell -File deploy/scripts/start-local-full.ps1 -StopOnly"
