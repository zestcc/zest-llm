# KB WireMock for http-knowledge (port 8091, avoids alert-mock :8090)
param([switch]$StopOnly)
$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$ToolsDir = Join-Path $Root "deploy\tools"
$Jar = Join-Path $ToolsDir "wiremock-standalone-3.3.1.jar"
$MockRoot = Join-Path $Root "deploy\kb-mock"
$PidFile = Join-Path $Root "deploy\logs\pids\kb-mock-local.pid"
$Port = 8091
if ($StopOnly) {
    if (Test-Path $PidFile) { Stop-Process -Id (Get-Content $PidFile) -Force -ErrorAction SilentlyContinue; Remove-Item $PidFile -Force }
    exit 0
}
if (-not (Test-Path $Jar)) { throw "Missing $Jar" }
Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
$p = Start-Process java -ArgumentList @("-jar",$Jar,"--port","$Port","--root-dir",$MockRoot,"--global-response-templating") -PassThru -WindowStyle Hidden
New-Item -Force -ItemType Directory -Path (Split-Path $PidFile) | Out-Null
$p.Id | Set-Content $PidFile
Start-Sleep -Seconds 2
Write-Host "KB mock :8091 OK"
