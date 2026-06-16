# 修复 Flyway V33 失败记录并重启 ZestLLM Admin（V33 会强制删除 order-service 重复 Review）
param(
    [string]$MysqlHost = "127.0.0.1",
    [int]$MysqlPort = 3306,
    [string]$Database = "zest_llm",
    [string]$MysqlUser = "root",
    [string]$AdminPort = "8088"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$LocalYml = Join-Path $Root "zest-llm-admin\src\main\resources\application-local.yml"
$Jar = Join-Path $Root "zest-llm-admin\target\zest-llm-admin-1.0.0.jar"

if (Test-Path $LocalYml) {
    if ($LocalYml -match "password:\s*(\S+)") { $MysqlPassword = $Matches[1] }
}

$mysqlExe = $null
foreach ($p in @(
    "mysql",
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
)) {
    if ($p -eq "mysql") {
        $cmd = Get-Command mysql -ErrorAction SilentlyContinue
        if ($cmd) { $mysqlExe = $cmd.Source; break }
    } elseif (Test-Path $p) { $mysqlExe = $p; break }
}

if (-not $mysqlExe) {
    Write-Error "mysql client not found. Install MySQL client or add mysql.exe to PATH."
}

Write-Host "Repairing failed Flyway V33 entry..." -ForegroundColor Yellow
$repairSql = "DELETE FROM flyway_schema_history WHERE version = '33' AND success = 0;"
& $mysqlExe -h $MysqlHost -P $MysqlPort -u $MysqlUser "-p$MysqlPassword" $Database -e $repairSql

Write-Host "Stopping existing admin on :$AdminPort..." -ForegroundColor Yellow
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
    if ($_.CommandLine -match 'zest-llm-admin.*\.jar' -and $_.CommandLine -match "port=$AdminPort") {
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
}
Start-Sleep -Seconds 2

Write-Host "Building admin jar..." -ForegroundColor Cyan
Set-Location $Root
mvn -pl zest-llm-admin -am package -DskipTests -q

Write-Host "Starting admin (Flyway V33 will run)..." -ForegroundColor Cyan
$log = Join-Path $Root "deploy\logs\admin-local.log"
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { "java" }
Start-Process -FilePath $java -ArgumentList "-jar",$Jar,"--spring.profiles.active=local","--server.port=$AdminPort" -RedirectStandardOutput $log -RedirectStandardError (Join-Path $Root "deploy\logs\admin-local.err.log") -WindowStyle Hidden

for ($i = 0; $i -lt 45; $i++) {
    try {
        $login = Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:$AdminPort/api/admin/auth/login" -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}' -TimeoutSec 3
        if ($login.data.token) { break }
    } catch { Start-Sleep -Seconds 2 }
}

Write-Host "Running import -Publish for Review profile..." -ForegroundColor Cyan
& (Join-Path $Root "scripts\import-zestllm-quality-profiles.ps1") -Publish

Write-Host "Runtime check:" -ForegroundColor Green
$rt = Invoke-RestMethod -Uri "http://127.0.0.1:$AdminPort/v1/apps/zeststory/tasks" -Headers @{ Authorization = "Bearer zeststory-runtime-dev-token" }
$rt | Where-Object { $_.taskCode -eq "zestStoryReview" } | Format-Table taskCode,publishedVersion,profileStatus -AutoSize
Write-Host "Refresh Zestory AI Center connectivity check"
