# ZestLLM Back-Channel Logout E2E
param(
    [string]$SsoUrl = "http://localhost:9000",
    [string]$LlmUrl = "http://localhost:8088",
    [string]$Username = "admin",
    [string]$Password = "admin123"
)

$ErrorActionPreference = "Stop"
$passed = 0
$failed = 0

function Pass([string]$Name) {
    Write-Host "[PASS] $Name" -ForegroundColor Green
    $script:passed++
}

function Fail([string]$Name, [string]$Reason) {
    Write-Host "[FAIL] $Name - $Reason" -ForegroundColor Red
    $script:failed++
}

function Assert-AppsOk([string]$Token, [string]$Step) {
    $resp = Invoke-WebRequest -Uri "$LlmUrl/api/admin/apps?page=1&size=1" `
        -Headers @{ Authorization = "Bearer $Token" } -UseBasicParsing -TimeoutSec 15
    if ($resp.StatusCode -eq 200) {
        Pass $Step
        return $true
    }
    Fail $Step "unexpected status: $($resp.StatusCode)"
    return $false
}

Write-Host "ZestLLM Back-Channel Logout E2E"
Write-Host "  SSO: $SsoUrl"
Write-Host "  LLM: $LlmUrl"
Write-Host ""

# Step 0: establish SSO OAuth authorization for zest-llm-admin
try {
    $auth = Invoke-RestMethod -Uri "$LlmUrl/api/admin/auth/sso/authorize" -TimeoutSec 15
    $authUrl = $auth.data.authorizationUrl
    $jar = Join-Path $env:TEMP "zestllm-sso-oauth.txt"
    Remove-Item $jar -ErrorAction SilentlyContinue
    curl.exe -s -c $jar -b $jar -o NUL $authUrl | Out-Null
    curl.exe -s -c $jar -b $jar -o NUL -X POST "$SsoUrl/login" `
        -d "username=$Username&password=$Password" -H "Content-Type: application/x-www-form-urlencoded" | Out-Null
    $hdr = curl.exe -s -c $jar -b $jar -D - -o NUL --max-redirs 0 $authUrl
    if ($hdr -match "code=") { Pass "sso-oauth-authorization" } else { Fail "sso-oauth-authorization" "no code in redirect" }
} catch {
    Fail "sso-oauth-authorization" $_.Exception.Message
}

# Step 1: ZestLLM local login
try {
    $login = Invoke-RestMethod -Method Post -Uri "$LlmUrl/api/admin/auth/login" `
        -ContentType "application/json" -Body (@{ username = $Username; password = $Password } | ConvertTo-Json) -TimeoutSec 15
    if ($login.data.token) { Pass "zestllm-login" } else { Fail "zestllm-login" "no token" }
    $llmToken = $login.data.token
} catch {
    Fail "zestllm-login" $_.Exception.Message
    exit 1
}

# Step 2: protected API works before logout
if (-not (Assert-AppsOk $llmToken "pre-logout-apps")) { exit 1 }

# Step 3: SSO admin logout triggers backchannel
$ssoSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
try {
    $null = Invoke-RestMethod -Method Post -Uri "$SsoUrl/api/admin/auth/login" `
        -ContentType "application/json" -Body (@{ username = $Username; password = $Password } | ConvertTo-Json) `
        -WebSession $ssoSession -TimeoutSec 15
    Pass "sso-admin-login"

    try {
        $null = Invoke-WebRequest -Method Post -Uri "$SsoUrl/api/admin/auth/logout" -WebSession $ssoSession -UseBasicParsing -TimeoutSec 15
        Pass "sso-admin-logout-trigger"
    } catch {
        Fail "sso-admin-logout-trigger" $_.Exception.Message
    }
} catch {
    Fail "sso-logout-chain" $_.Exception.Message
}

Start-Sleep -Seconds 6

# Step 4: revoked JWT must be denied (HTTP 401/403)
try {
    $resp = Invoke-WebRequest -Uri "$LlmUrl/api/admin/apps?page=1&size=1" `
        -Headers @{ Authorization = "Bearer $llmToken" } -UseBasicParsing -TimeoutSec 15
    if ($resp.StatusCode -eq 401 -or $resp.StatusCode -eq 403) {
        Pass "post-logout-deny"
    } else {
        Fail "post-logout-deny" "still accessible after backchannel logout (status $($resp.StatusCode))"
    }
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    if ($code -eq 401 -or $code -eq 403) {
        Pass "post-logout-deny"
    } else {
        Fail "post-logout-deny" $_.Exception.Message
    }
}

Write-Host ""
Write-Host "Result: $passed passed, $failed failed"
if ($failed -gt 0) { exit 1 }
