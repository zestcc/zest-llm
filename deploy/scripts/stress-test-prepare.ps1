# ZestLLM prepare 压力测试（并发 Job）
param(
    [string]$AdminUrl = "http://localhost:8088",
    [int]$Concurrency = 50,
    [int]$Total = 200,
    [int]$P95MaxMs = 1200,
    [double]$MinSuccessRate = 0.95
)

$ErrorActionPreference = "Continue"
$body = '{"appKey":"order-service","code":"aiChat","inputs":{"question":"stress"}}'
$headers = @{ Authorization = "Bearer demo-token-123"; "Content-Type" = "application/json" }
$latencies = New-Object System.Collections.Generic.List[int]
$success = 0
$fail = 0

function Invoke-PrepareOnce {
    param([string]$Url, [hashtable]$Hdrs, [string]$ReqBody)
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $null = Invoke-RestMethod -Uri "$Url/v1/llm/prepare" -Method POST -Body $ReqBody -Headers $Hdrs -TimeoutSec 30
        $sw.Stop()
        return @{ Ok = $true; Ms = [int]$sw.ElapsedMilliseconds }
    } catch {
        $sw.Stop()
        return @{ Ok = $false; Ms = [int]$sw.ElapsedMilliseconds }
    }
}

Write-Host "=== Stress Test prepare Concurrency=$Concurrency Total=$Total ==="
$jobs = @()
for ($i = 0; $i -lt $Total; $i++) {
    while ((Get-Job -State Running).Count -ge $Concurrency) {
        Start-Sleep -Milliseconds 50
        Get-Job -State Completed | Receive-Job | ForEach-Object {
            if ($_.Ok) {
                $latencies.Add($_.Ms)
                $script:success++
            } else {
                $script:fail++
            }
        }
        Get-Job -State Completed | Remove-Job -Force
    }
    $jobs += Start-Job -ScriptBlock ${function:Invoke-PrepareOnce} -ArgumentList $AdminUrl, $headers, $body
}
Wait-Job -Job $jobs | Out-Null
Get-Job | Receive-Job | ForEach-Object {
    if ($_.Ok) {
        $latencies.Add($_.Ms)
        $script:success++
    } else {
        $script:fail++
    }
}
Remove-Job -Job $jobs -Force

$rate = if (($success + $fail) -gt 0) { $success / ($success + $fail) } else { 0 }
$sorted = @($latencies | Sort-Object)
$p50 = 0
$p95 = 0
if ($sorted.Count -gt 0) {
    $p50Idx = [int][Math]::Floor($sorted.Count * 0.5)
    $p95Idx = [int][Math]::Min($sorted.Count - 1, [Math]::Floor($sorted.Count * 0.95))
    $p50 = $sorted[$p50Idx]
    $p95 = $sorted[$p95Idx]
}

Write-Host "Success=$success Fail=$fail Rate=$([math]::Round($rate * 100, 1))%"
Write-Host "Samples=$($sorted.Count) P50=${p50}ms P95=${p95}ms max=$($sorted[-1])ms"
if ($rate -lt $MinSuccessRate) { Write-Host "FAIL success rate below $MinSuccessRate"; exit 1 }
if ($sorted.Count -gt 0 -and $p95 -gt $P95MaxMs) { Write-Host "FAIL P95 ${p95}ms > ${P95MaxMs}ms"; exit 1 }
Write-Host "PASS stress test"
exit 0
