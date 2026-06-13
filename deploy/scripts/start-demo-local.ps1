# Windows 本地启动 order-service Demo（无需 Docker）
# 编码：请用 UTF-8 with BOM 保存本文件，避免中文注释与下一行代码被 PowerShell 误解析。
# 用法: powershell -File deploy/scripts/start-demo-local.ps1 [-StopOnly] [-SkipBuild]
param(
    [switch]$StopOnly,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$DemoDir = Join-Path $Root "zest-llm-demo"
$LocalYml = Join-Path $DemoDir "src\main\resources\application-local.yml"
$ExampleYml = Join-Path $DemoDir "src\main\resources\application-local.example.yml"
$Jar = Join-Path $DemoDir "target\zest-llm-demo-1.0.0.jar"
$LogDir = Join-Path $Root "deploy\logs"
$LogFile = Join-Path $LogDir "demo-local.log"
$LogErr = Join-Path $LogDir "demo-local.err.log"
$PidFile = Join-Path $LogDir "pids\demo-local.pid"
$PortCheckUrl = "http://127.0.0.1:8081/"
$MethodAUrl = "http://127.0.0.1:8081/demo/order/methodA?orderId=1&question=ping"

function Stop-Demo {
    if (Test-Path $PidFile) {
        $raw = Get-Content $PidFile -ErrorAction SilentlyContinue
        if ($raw -match '^\d+$') {
            $procId = [int]$raw
            $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
            if ($proc) {
                Write-Host "Stopping Demo PID $procId" -ForegroundColor Yellow
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            }
        }
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    }
    try {
        $conns = Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue
        foreach ($c in $conns) {
            $owningPid = $c.OwningProcess
            if ($owningPid -and $owningPid -gt 0) {
                Write-Host "Freeing port 8081 (PID $owningPid)" -ForegroundColor Yellow
                Stop-Process -Id $owningPid -Force -ErrorAction SilentlyContinue
            }
        }
    } catch { }
}

function Test-DemoPort {
    try {
        $null = Invoke-WebRequest -Uri $PortCheckUrl -UseBasicParsing -TimeoutSec 5
        return $true
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode.value__ -ge 400) {
            return $true
        }
        return $false
    }
}

function Test-DemoMethodA {
    try {
        $r = Invoke-WebRequest -Uri $MethodAUrl -UseBasicParsing -TimeoutSec 30
        if ($r.StatusCode -ne 200) { return $false }
        return ($r.Content -match 'traceId' -and $r.Content -match 'answer')
    } catch {
        return $false
    }
}

if ($StopOnly) {
    Stop-Demo
    Write-Host "Demo stopped." -ForegroundColor Green
    exit 0
}

New-Item -ItemType Directory -Force -Path $LogDir, (Join-Path $LogDir "pids") | Out-Null

function Ensure-DemoLocalConfig {
    if (-not (Test-Path $ExampleYml)) { throw "Missing $ExampleYml" }
    if (-not (Test-Path $LocalYml)) {
        Copy-Item $ExampleYml $LocalYml
        Write-Host "Created demo application-local.yml — edit MySQL password if needed." -ForegroundColor Yellow
        return
    }
    $content = Get-Content $LocalYml -Raw -Encoding UTF8
    $agentAuthOk = $content -match '(?ms)\n\s+agent:\s*\n(?:[^\n]+\n)*?\s+auth-token:'
    if ($agentAuthOk) { return }
    Write-Host "WARN: demo application-local.yml lacks zest.llm.agent auth-token — syncing from example (agent auth, litellm-api-key, registry logging)." -ForegroundColor Yellow
    $savedPwd = $null
    if ($content -match '(?m)^(\s+password:\s*.+)$') { $savedPwd = $Matches[1] }
    Copy-Item $ExampleYml $LocalYml -Force
    if ($savedPwd) {
        $newContent = Get-Content $LocalYml -Raw -Encoding UTF8
        $newContent = $newContent -replace '(?m)^(\s+password:\s*).+$', $savedPwd
        [System.IO.File]::WriteAllText($LocalYml, $newContent)
    }
}

Ensure-DemoLocalConfig

if (-not $SkipBuild) {
    Write-Host "== mvn package zest-llm-demo (skip tests) ==" -ForegroundColor Cyan
    Push-Location $Root
    mvn -pl zest-llm-demo -am package -DskipTests -q
    Pop-Location
}

if (-not (Test-Path $Jar)) {
    throw "Missing jar: $Jar — run: mvn -pl zest-llm-demo -am package -DskipTests"
}

Stop-Demo
Start-Sleep -Seconds 1

if (Test-DemoPort) {
    if (Test-DemoMethodA) {
        Write-Host "Demo already up: http://127.0.0.1:8081" -ForegroundColor Green
        exit 0
    }
    Write-Host "Demo port :8081 is up but methodA not ready — restarting with fresh jar/config." -ForegroundColor Yellow
}

Write-Host "== Starting Demo (:8081) ==" -ForegroundColor Cyan
Write-Host "Requires Admin :8088 (+ LiteLLM :4000 for AI answer)"

"" | Set-Content $LogFile -Encoding UTF8
"" | Set-Content $LogErr -Encoding UTF8

$proc = Start-Process -FilePath "java" `
    -ArgumentList @("-jar", $Jar, "--spring.profiles.active=local") `
    -WorkingDirectory $DemoDir `
    -RedirectStandardOutput $LogFile `
    -RedirectStandardError $LogErr `
    -PassThru -WindowStyle Hidden
$proc.Id | Set-Content $PidFile

$portOk = $false
for ($i = 1; $i -le 60; $i++) {
    if ($proc.HasExited) {
        Write-Host "Demo exited early (code $($proc.ExitCode))." -ForegroundColor Red
        Get-Content $LogErr -Tail 40 -ErrorAction SilentlyContinue
        throw "Demo failed to start — see deploy/logs/demo-local.err.log"
    }
    if (Test-DemoPort) {
        $portOk = $true
        break
    }
    Write-Host "  waiting for Tomcat :8081 ... ($i/60)" -ForegroundColor DarkGray
    Start-Sleep -Seconds 2
}

if (-not $portOk) {
    Get-Content $LogErr -Tail 40 -ErrorAction SilentlyContinue
    throw "Demo Tomcat timeout — see deploy/logs/demo-local.err.log"
}

Write-Host "Demo Tomcat is up: http://127.0.0.1:8081" -ForegroundColor Green

$methodAOk = $false
for ($i = 1; $i -le 15; $i++) {
    if (Test-DemoMethodA) {
        $methodAOk = $true
        break
    }
    Write-Host "  waiting for methodA (needs Admin :8088 + LiteLLM :4000) ... ($i/15)" -ForegroundColor DarkGray
    Start-Sleep -Seconds 2
}

if ($methodAOk) {
    Write-Host "methodA smoke OK (traceId + answer)" -ForegroundColor Green
} else {
    Write-Host "methodA not ready yet — start Admin/LiteLLM first, then retry:" -ForegroundColor Yellow
    Write-Host "  $MethodAUrl"
}

Write-Host "Try: $MethodAUrl"
Write-Host "Log:  $LogFile"
Write-Host "Stop: powershell -File deploy/scripts/start-demo-local.ps1 -StopOnly"
