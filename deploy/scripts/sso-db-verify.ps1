# SSO 浏览器联调后只读验证（Windows）
# 用法: powershell -File deploy/scripts/sso-db-verify.ps1
# 环境: $env:MYSQL_HOST $env:MYSQL_PASSWORD 等（同 sso-db-verify.sh）
param(
    [string]$MysqlHost = $env:MYSQL_HOST,
    [string]$MysqlPort = $(if ($env:MYSQL_PORT) { $env:MYSQL_PORT } else { "3306" }),
    [string]$MysqlUser = $(if ($env:MYSQL_USER) { $env:MYSQL_USER } else { "root" }),
    [string]$MysqlPassword = $env:MYSQL_PASSWORD,
    [string]$Database = $(if ($env:MYSQL_DATABASE) { $env:MYSQL_DATABASE } else { "zest_llm" }),
    [int]$Limit = 10
)

$ErrorActionPreference = "Stop"
if (-not $MysqlHost) { $MysqlHost = "127.0.0.1" }

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysql) {
    Write-Host "SKIP: mysql client not found — run SQL from deploy/scripts/sso-browser-checklist.md" -ForegroundColor Yellow
    exit 0
}
if ([string]::IsNullOrWhiteSpace($MysqlPassword)) {
    Write-Host "SKIP: MYSQL_PASSWORD not set" -ForegroundColor Yellow
    exit 0
}

Write-Host "== SSO DB verify (read-only) ==" -ForegroundColor Cyan
Write-Host "Host=${MysqlHost}:${MysqlPort} DB=$Database"

$sql = @"
SELECT id, username, sso_provider, sso_subject, email, updated_at
FROM llm_admin_user
WHERE sso_provider IS NOT NULL
ORDER BY updated_at DESC
LIMIT $Limit;
"@

$env:MYSQL_PWD = $MysqlPassword
& mysql -h $MysqlHost -P $MysqlPort -u $MysqlUser $Database -e $sql

$countSql = "SELECT COUNT(*) FROM llm_admin_user WHERE sso_provider IS NOT NULL AND sso_subject IS NOT NULL AND sso_subject <> '';"
$count = (& mysql -h $MysqlHost -P $MysqlPort -u $MysqlUser $Database -N -e $countSql).Trim()
Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue

if ([int]$count -gt 0) {
    Write-Host "PASS sso-db-verify — $count user(s) with sso_subject" -ForegroundColor Green
    exit 0
}
Write-Host "WARN sso-db-verify — no sso_subject rows; complete browser SSO login first" -ForegroundColor Yellow
exit 1
