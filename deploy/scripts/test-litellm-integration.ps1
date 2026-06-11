$headers = @{
    Authorization = "Bearer demo-token-123"
    "Content-Type" = "application/json"
}
$body = '{"appKey":"order-service","code":"aiChat","inputs":{"question":"hello"}}'
Write-Host "== prepare =="
try {
    $prep = Invoke-RestMethod -Uri "http://127.0.0.1:8088/v1/llm/prepare" -Method POST -Headers $headers -Body $body -TimeoutSec 15
    $prep | ConvertTo-Json -Depth 6
} catch {
    Write-Host "FAIL:" $_.Exception.Message
    exit 1
}

Write-Host ""
Write-Host "== direct LiteLLM chat =="
$llmHeaders = @{
    Authorization = "Bearer sk-zest-llm-demo"
    "Content-Type" = "application/json"
}
$chatBody = '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"hi"}],"max_tokens":10}'
try {
    $chat = Invoke-RestMethod -Uri "http://127.0.0.1:4000/v1/chat/completions" -Method POST -Headers $llmHeaders -Body $chatBody -TimeoutSec 30
    Write-Host "LiteLLM direct OK:" $chat.choices[0].message.content
} catch {
    Write-Host "LiteLLM direct failed:" $_.Exception.Message
    if ($_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message }
}
