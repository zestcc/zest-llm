# 导入 ZestStory 质量作业（Prompt + ModelRoute + AgentProfile）到 ZestLLM
# 用法: .\scripts\import-zestllm-quality-profiles.ps1 [-DryRun] [-Publish] [-FixPrompts]
#
# Review / Revise 使用不同的 Prompt 文件（角色与输出契约不同）

param(
    [string]$ControlPlaneUrl = "http://127.0.0.1:8088",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin123",
    [string]$AppKey = "zeststory",
    [switch]$DryRun,
    [switch]$Publish,
    [switch]$FixPrompts
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$examplesDir = Join-Path $root "deploy\examples"
$importFile = Join-Path $examplesDir "zeststory-quality-profiles.import.json"

if (-not (Test-Path $importFile)) { Write-Error "Import file not found: $importFile" }

$Utf8NoBom = New-Object System.Text.UTF8Encoding $false

function AdminGet([string]$Token, [string]$Path) {
    return Invoke-RestMethod -Method Get -Uri "$ControlPlaneUrl$Path" -Headers @{ Authorization = "Bearer $Token" }
}

function AdminPost([string]$Token, [string]$Path, [string]$JsonBody) {
    return Invoke-RestMethod -Method Post -Uri "$ControlPlaneUrl$Path" -Headers @{ Authorization = "Bearer $Token" } -Body $JsonBody -ContentType "application/json"
}

function AdminPut([string]$Token, [string]$Path, [string]$JsonBody) {
    try {
        return Invoke-RestMethod -Method Put -Uri "$ControlPlaneUrl$Path" -Headers @{ Authorization = "Bearer $Token" } -Body $JsonBody -ContentType "application/json"
    } catch {
        $detail = $_.ErrorDetails.Message
        if ($detail) { throw "$($_.Exception.Message) :: $detail" }
        throw
    }
}

function Warn-DuplicateTasks([string]$Token, [string]$AppKey) {
    try {
        $path = if ($AppKey) { "/api/admin/tasks?page=1&size=200&appKey=$AppKey" } else { "/api/admin/tasks?page=1&size=200" }
        $page = AdminGet $Token $path
        $records = @($page.data.records)
        if ($records.Count -eq 0 -and $AppKey) {
            $page = AdminGet $Token "/api/admin/tasks?page=1&size=200"
            $records = @($page.data.records)
        }
        foreach ($g in ($records | Group-Object -Property code)) {
            if ($g.Count -le 1) { continue }
            $inactive = @($g.Group | Where-Object { $_.status -ne "ACTIVE" })
            if ($inactive.Count -gt 0) {
                Write-Host "WARN: duplicate taskCode '$($g.Name)' ($($g.Count) rows). Delete INACTIVE in Admin or restart ZestLLM Admin to apply Flyway V33 merge." -ForegroundColor Yellow
            }
        }
    } catch {
        Write-Host "WARN: could not scan duplicate tasks: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

function Ensure-AgentProfile([string]$TaskCode, [string]$Token, [string]$Version, [string]$ProfileJson, [switch]$DoPublish) {
    $profiles = @(AdminGet $Token "/api/admin/agent-profiles/$TaskCode/versions").data
    $existing = $profiles | Where-Object { $_.version -eq $Version } | Select-Object -First 1
    $published = $profiles | Where-Object { $_.status -eq "PUBLISHED" } | Select-Object -First 1

    if ($existing -and $existing.status -eq "PUBLISHED") {
        Write-Host "  profile $Version already published (skip update)"
        if ($DoPublish) {
            Write-Host "  publish skipped (already published)"
        }
        return "already-published"
    }

    if ($existing) {
        Write-Host "  update profile $Version (draft)"
        $body = (@{ profileJson = $ProfileJson; providerPresetCode = "litellm-local"; runtimeMode = "agent" } | ConvertTo-Json -Compress)
        AdminPut $Token "/api/admin/agent-profiles/$TaskCode/versions/$Version" $body | Out-Null
        $action = "updated"
    } else {
        Write-Host "  create profile $Version"
        $body = (@{ version = $Version; profileJson = $ProfileJson; providerPresetCode = "litellm-local"; runtimeMode = "agent" } | ConvertTo-Json -Compress)
        AdminPost $Token "/api/admin/agent-profiles/$TaskCode/versions" $body | Out-Null
        $action = "created"
    }

    if ($DoPublish) {
        if ($published -and $published.version -eq $Version) {
            Write-Host "  publish skipped (already published)"
            return "$action+published"
        }
        Write-Host "  publish profile $Version"
        AdminPost $Token "/api/admin/agent-profiles/$TaskCode/publish" (@{ version = $Version; operator = "zeststory-quality-import" } | ConvertTo-Json -Compress) | Out-Null
        return "$action+published"
    }
    return $action
}

function Load-PromptPostJson([string]$FileName) {
    $path = Join-Path $examplesDir $FileName
    if (-not (Test-Path $path)) { Write-Error "Prompt POST JSON not found: $path" }
    return [System.IO.File]::ReadAllText($path, $Utf8NoBom).Trim()
}

function Ensure-PromptVersion([string]$TaskCode, [string]$Token, [string]$PromptPostJson, [switch]$ForceUpgrade) {
    $list = @(AdminGet $Token "/api/admin/prompts/$TaskCode/versions").data
    $existingVersions = @($list | ForEach-Object { $_.version })

    if (-not $ForceUpgrade) {
        $published = $list | Where-Object { $_.status -eq "PUBLISHED" } | Select-Object -First 1
        if ($published) {
            Write-Host "  prompt published $($published.version)"
            return
        }
    }

    $createdVersion = $null
    foreach ($ver in @("v6", "v7", "v8", "v9", "v10")) {
        if ($existingVersions -contains $ver) { continue }
        Write-Host "  create prompt $ver (task-specific)"
        $createJson = $PromptPostJson -replace '"version"\s*:\s*"v2"', "`"version`":`"$ver`""
        try {
            AdminPost $Token "/api/admin/prompts/$TaskCode/versions" $createJson | Out-Null
            $createdVersion = $ver
            break
        } catch {
            Write-Host "  create $ver failed: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }

    if (-not $createdVersion) {
        $fallback = ($list | Sort-Object { [int]($_.version -replace '\D','') } -Descending | Select-Object -First 1)
        if ($fallback) {
            $createdVersion = $fallback.version
            Write-Host "  reuse existing $createdVersion (no free slot)" -ForegroundColor Yellow
        } else {
            Write-Host "  cannot create prompt version" -ForegroundColor Red
            return
        }
    }

    Write-Host "  publish prompt $createdVersion"
    $pubJson = (@{ version = $createdVersion; operator = "zeststory-quality-import" } | ConvertTo-Json -Compress)
    AdminPost $Token "/api/admin/prompts/$TaskCode/publish" $pubJson | Out-Null
}

Write-Host "ZestLLM control plane: $ControlPlaneUrl"
Write-Host "Import file: $importFile"

$loginJson = (@{ username = $AdminUser; password = $AdminPassword } | ConvertTo-Json -Compress)
$login = Invoke-RestMethod -Method Post -Uri "$ControlPlaneUrl/api/admin/auth/login" -Body $loginJson -ContentType "application/json"
$token = $login.data.token
if (-not $token) { Write-Error "Admin login failed" }

Warn-DuplicateTasks -Token $token -AppKey $AppKey

$payload = ([System.IO.File]::ReadAllText($importFile, $Utf8NoBom) | ConvertFrom-Json)

if ($DryRun) {
    foreach ($item in $payload.items) {
        Write-Host "[dry-run] $($item.taskCode) prompt=$($item.promptPostFile)"
    }
    return
}

$results = @()
foreach ($item in $payload.items) {
    $taskCode = $item.taskCode
    $version = if ($item.version) { $item.version } else { "v1" }
    $promptJson = Load-PromptPostJson $item.promptPostFile

    Write-Host ""
    Write-Host "== $taskCode ($($item.promptPostFile)) ==" -ForegroundColor Cyan

    Ensure-PromptVersion -TaskCode $taskCode -Token $token -PromptPostJson $promptJson -ForceUpgrade:$FixPrompts

    if (-not $FixPrompts) {
        Write-Host "  upsert model route"
        $routeJson = (@{
            primaryModel = "deepseek-v4-flash"; fallbackModels = "deepseek-v4-pro"
            maxTokens = [int]$item.maxTokens; temperature = [double]$item.temperature
            timeoutMs = 120000; policyJson = "{}"
        } | ConvertTo-Json -Compress)
        AdminPut $token "/api/admin/model-routes/$taskCode" $routeJson | Out-Null

        $action = Ensure-AgentProfile -TaskCode $taskCode -Token $token -Version $version -ProfileJson $item.profileJson -DoPublish:$Publish
    } else {
        $action = "prompt-upgraded"
    }
    $results += [pscustomobject]@{ taskCode = $taskCode; promptFile = $item.promptPostFile; action = $action }
}

Write-Host ""
$results | Format-Table -AutoSize
Write-Host "Review prompt should mention: senior editor + JSON output"
Write-Host "Revise prompt should mention: senior editor + revised prose only"
