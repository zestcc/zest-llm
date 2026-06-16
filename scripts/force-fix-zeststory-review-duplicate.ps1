# 强制清理 order-service / INACTIVE 重复的 zestStoryReview，并确保 zeststory ACTIVE 有 Profile
# 用法: .\scripts\force-fix-zeststory-review-duplicate.ps1
# 前置: ZestLLM Admin 已重启（含 V34 迁移 + force delete API）

param(
    [string]$ControlPlaneUrl = "http://127.0.0.1:8088",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin123"
)

$ErrorActionPreference = "Stop"

function AdminGet([string]$Token, [string]$Path) {
    return Invoke-RestMethod -Method Get -Uri "$ControlPlaneUrl$Path" -Headers @{ Authorization = "Bearer $Token" }
}

function AdminDelete([string]$Token, [string]$Path) {
    return Invoke-RestMethod -Method Delete -Uri "$ControlPlaneUrl$Path" -Headers @{ Authorization = "Bearer $Token" }
}

$login = Invoke-RestMethod -Method Post -Uri "$ControlPlaneUrl/api/admin/auth/login" `
    -Body (@{ username = $AdminUser; password = $AdminPassword } | ConvertTo-Json -Compress) `
    -ContentType "application/json"
$token = $login.data.token
if (-not $token) { Write-Error "Admin login failed" }

Write-Host "=== Before ===" -ForegroundColor Cyan
$page = AdminGet $token "/api/admin/tasks?page=1&size=200"
$page.data.records | Where-Object { $_.code -eq "zestStoryReview" } | Format-Table id,appKey,code,status,name -AutoSize

$orphans = @($page.data.records | Where-Object {
    $_.code -eq "zestStoryReview" -and ($_.status -ne "ACTIVE" -or $_.appKey -eq "order-service")
})

foreach ($t in $orphans) {
    Write-Host "Force delete task id=$($t.id) app=$($t.appKey) status=$($t.status)" -ForegroundColor Yellow
    try {
        AdminDelete $token "/api/admin/tasks/by-id/$($t.id)/force" | Out-Null
        Write-Host "  deleted" -ForegroundColor Green
    } catch {
        Write-Host "  by-id failed, try app+code: $($_.Exception.Message)" -ForegroundColor Yellow
        if ($t.appKey) {
            AdminDelete $token "/api/admin/tasks/force?appKey=$($t.appKey)&code=$($t.code)" | Out-Null
            Write-Host "  deleted via app+code" -ForegroundColor Green
        } else {
            throw
        }
    }
}

Write-Host "`n=== Re-import profile on zeststory ACTIVE ===" -ForegroundColor Cyan
& (Join-Path (Split-Path -Parent $PSScriptRoot) "scripts\import-zestllm-quality-profiles.ps1") -Publish

Write-Host "`n=== Runtime check ===" -ForegroundColor Cyan
$rt = Invoke-RestMethod -Uri "$ControlPlaneUrl/v1/apps/zeststory/tasks" `
    -Headers @{ Authorization = "Bearer zeststory-runtime-dev-token" }
$rt | Where-Object { $_.taskCode -eq "zestStoryReview" } | Format-Table taskCode,publishedVersion,profileStatus,overallStatus -AutoSize

Write-Host "Done. Refresh Zestory AI Center -> 检测连通性" -ForegroundColor Green
