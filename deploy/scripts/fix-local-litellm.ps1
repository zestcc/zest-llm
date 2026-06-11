# Local LiteLLM: point provider to localhost:4000 and test invoke
$AdminUrl = "http://127.0.0.1:8088"
$LiteLLMKey = "sk-zest-llm-demo"

$login = Invoke-RestMethod -Uri "$AdminUrl/api/admin/auth/login" -Method POST `
    -Body '{"username":"admin","password":"admin123"}' -ContentType "application/json"
$token = $login.data.token
if (-not $token) { $token = $login.token }
$h = @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" }

Write-Host "== Update litellm-default -> localhost:4000 =="
$updateBody = '{"presetName":"LiteLLM local","providerType":"litellm","authMode":"API_KEY","configJson":"{\"type\":\"litellm\",\"baseUrl\":\"http://localhost:4000\",\"protocol\":\"openai\"}"}'
try {
    Invoke-RestMethod -Uri "$AdminUrl/api/admin/provider-presets/litellm-default" -Method PUT -Headers $h -Body $updateBody -TimeoutSec 15 | Out-Null
    Write-Host "Provider updated."
} catch {
    Write-Host "Provider update failed:" $_.Exception.Message
}

$env:LITELLM_API_KEY = $LiteLLMKey

Write-Host ""
Write-Host "== invoke =="
$runtimeHeaders = @{
    Authorization = "Bearer demo-token-123"
    "Content-Type" = "application/json"
}
$invokeBody = '{"appKey":"order-service","code":"aiChat","inputs":{"question":"say hi in one word"}}'
try {
    $resp = Invoke-RestMethod -Uri "$AdminUrl/v1/llm/invoke" -Method POST -Headers $runtimeHeaders -Body $invokeBody -TimeoutSec 60
    Write-Host "traceId:" $resp.traceId
    Write-Host "answer:" $resp.output.answer
    Write-Host "status:" $resp.status
    Write-Host "Integration OK."
} catch {
    Write-Host "invoke failed:" $_.Exception.Message
    if ($_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message }
    Write-Host "Restart Admin with: `$env:LITELLM_API_KEY='sk-zest-llm-demo'"
    exit 1
}
