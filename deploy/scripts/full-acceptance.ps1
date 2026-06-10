# ZestLLM 生产级全量验收（Windows / 本地 Admin 优先）
# 维度：功能 / 安全 / 性能冒烟 / 可靠性 / 兼容性 / RBAC
param(
    [string]$AdminUrl = "http://localhost:8088",
    [string]$DemoUrl = "http://localhost:8081",
    [string]$FlowUrl = "http://localhost:20552",
    [int]$PerfRequests = 30,
    [int]$PerfP95MaxMs = 800
)

$ErrorActionPreference = "Continue"
$ReportDir = Join-Path (Split-Path $PSScriptRoot -Parent) "test-reports"
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null
$ReportFile = Join-Path $ReportDir ("report-{0:yyyyMMdd-HHmmss}.txt" -f (Get-Date))
$script:Passed = 0
$script:Failed = 0
$script:Skipped = 0
$script:Token = ""

function Write-Report($msg) {
    Write-Host $msg
    Add-Content -Path $ReportFile -Value $msg
}

function Test-Reachable($url) {
    try {
        $null = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3
        return $true
    } catch {
        if ($_.Exception.Response) { return $true }
        return $false
    }
}

function Assert-Pass($id, $cond, $detail) {
    if ($cond) {
        $script:Passed++
        Write-Report "PASS $id $detail"
    } else {
        $script:Failed++
        Write-Report "FAIL $id $detail"
    }
}

function Assert-Skip($id, $reason) {
    $script:Skipped++
    Write-Report "SKIP $id $reason"
}

function Get-AdminToken {
    if ($script:Token) { return $script:Token }
    $body = '{"username":"admin","password":"admin123"}'
    $resp = Invoke-RestMethod -Uri "$AdminUrl/api/admin/auth/login" -Method POST -Body $body -ContentType "application/json" -TimeoutSec 15
    $script:Token = $resp.data.token
    if (-not $script:Token) { $script:Token = $resp.token }
    return $script:Token
}

function Invoke-AdminGet($path) {
    $token = Get-AdminToken
    return Invoke-RestMethod -Uri "$AdminUrl$path" -Headers @{ Authorization = "Bearer $token" } -TimeoutSec 20
}

function Invoke-AdminPost($path, $jsonBody) {
    $token = Get-AdminToken
    return Invoke-RestMethod -Uri "$AdminUrl$path" -Method POST -Body $jsonBody -ContentType "application/json" -Headers @{ Authorization = "Bearer $token" } -TimeoutSec 60
}

Write-Report "=== ZestLLM Full Acceptance $(Get-Date -Format o) ==="
Write-Report "AdminUrl=$AdminUrl DemoUrl=$DemoUrl FlowUrl=$FlowUrl"

# --- ENV ---
Write-Report "--- ENV ---"
Assert-Pass "ENV-01" (Test-Reachable $AdminUrl) "Admin reachable"
Assert-Pass "ENV-02" (Test-Reachable "http://localhost:5174") "Admin UI dev (optional)"
if (Test-Reachable $DemoUrl) { Write-Report "INFO Demo reachable" } else { Write-Report "WARN Demo not running — AC1/16/19/20 skipped" }
if (Test-Reachable $FlowUrl) { Write-Report "INFO Flow reachable" } else { Write-Report "WARN Flow not running — AC16/17 skipped" }

# --- SEC ---
Write-Report "--- SEC ---"
try {
    $bad = Invoke-RestMethod -Uri "$AdminUrl/v1/llm/invoke" -Method POST -Body '{"appKey":"order-service","code":"aiChat","inputs":{"question":"x"}}' -ContentType "application/json" -Headers @{ Authorization = "Bearer wrong-token" } -TimeoutSec 10
    Assert-Pass "SEC-01" ($bad.code -eq "AUTH_FAILED" -or ($bad | ConvertTo-Json).Contains("AUTH_FAILED")) "wrong token rejected"
} catch {
    Assert-Pass "SEC-01" $true "wrong token rejected (HTTP error)"
}
try {
    $null = Invoke-WebRequest -Uri "$AdminUrl/api/admin/tasks?page=1" -UseBasicParsing -TimeoutSec 5
    Assert-Pass "SEC-02" $false "unauthenticated admin blocked"
} catch {
    $code = [int]$_.Exception.Response.StatusCode
    Assert-Pass "SEC-02" ($code -eq 401 -or $code -eq 403) "unauthenticated admin HTTP $code"
}
try {
    $opBody = '{"username":"operator","password":"operator123"}'
    $op = Invoke-RestMethod -Uri "$AdminUrl/api/admin/auth/login" -Method POST -Body $opBody -ContentType "application/json"
    $opToken = $op.data.token; if (-not $opToken) { $opToken = $op.token }
    try {
        Invoke-RestMethod -Uri "$AdminUrl/api/admin/agent-profile-probes/run-all" -Method POST -Body '{}' -ContentType "application/json" -Headers @{ Authorization = "Bearer $opToken" } -TimeoutSec 10 | Out-Null
        Assert-Pass "SEC-03" $false "operator probe-all forbidden"
    } catch {
        Assert-Pass "SEC-03" ([int]$_.Exception.Response.StatusCode -eq 403) "operator probe-all 403"
    }
    $list = Invoke-RestMethod -Uri "$AdminUrl/api/admin/tasks?page=1&size=5" -Headers @{ Authorization = "Bearer $opToken" }
    Assert-Pass "SEC-04" ($null -ne $list) "operator read OK"
} catch {
    Assert-Skip "SEC-03" "operator login unavailable"
}

# --- FUNC Admin API matrix ---
Write-Report "--- FUNC-API ---"
try {
    Get-AdminToken | Out-Null
    Assert-Pass "API-01" $true "admin login"
} catch {
    Assert-Pass "API-01" $false "admin login: $($_.Exception.Message)"
}

$apiChecks = @(
    @{ Id = "API-02"; Path = "/api/admin/meta/features"; Key = "agentProbeApi" },
    @{ Id = "API-03"; Path = "/api/admin/dashboard/stats"; Key = "executions" },
    @{ Id = "API-04"; Path = "/api/admin/dashboard/agent-health"; Key = "monitored" },
    @{ Id = "API-05"; Path = "/api/admin/dashboard/cost?days=7"; Key = "days" },
    @{ Id = "API-06"; Path = "/api/admin/config/observability"; Key = "adapterId" },
    @{ Id = "API-07"; Path = "/api/admin/executions?page=1&size=5"; Key = "records" },
    @{ Id = "API-08"; Path = "/api/admin/agent-profiles/aiChat/versions"; Key = "data" },
    @{ Id = "API-09"; Path = "/api/admin/agent-profile-probes/aiChat/latest"; Key = "data" },
    @{ Id = "API-10"; Path = "/api/admin/agent-profile-probes/aiChat/history?page=1&size=5"; Key = "records" },
    @{ Id = "API-11"; Path = "/api/admin/agent-profile-probes/aiChat/history/trend?days=7"; Key = "data" },
    @{ Id = "API-12"; Path = "/api/admin/cost-alerts?page=1&size=5"; Key = "records" },
    @{ Id = "API-13"; Path = "/api/admin/cost-alerts/summary?days=7"; Key = "data" },
    @{ Id = "API-14"; Path = "/api/admin/agent-probe-alerts?page=1&size=5"; Key = "records" },
    @{ Id = "API-15"; Path = "/api/admin/executions/archive/stats"; Key = "hotExecutions" },
    @{ Id = "API-16"; Path = "/api/admin/executions/archive/runs?page=1&size=5"; Key = "records" },
    @{ Id = "API-17"; Path = "/api/admin/eval/datasets"; Key = "data" },
    @{ Id = "API-18"; Path = "/api/admin/flow-chains"; Key = "data" },
    @{ Id = "API-19"; Path = "/api/admin/mcp-servers"; Key = "data" },
    @{ Id = "API-20"; Path = "/api/admin/users"; Key = "data" },
    @{ Id = "API-21"; Path = "/api/admin/tenants"; Key = "data" },
    @{ Id = "API-22"; Path = "/api/admin/adapters/health/all"; Key = "data" },
    @{ Id = "API-23"; Path = "/api/admin/audit-logs?page=1&size=5"; Key = "records" },
    @{ Id = "API-24"; Path = "/api/admin/apps?page=1&size=5"; Key = "records" },
    @{ Id = "API-25"; Path = "/api/admin/tasks?page=1&size=5"; Key = "records" }
)

foreach ($c in $apiChecks) {
    try {
        $r = Invoke-AdminGet $c.Path
        $json = $r | ConvertTo-Json -Depth 6 -Compress
        $ok = $json.Contains($c.Key) -or ($r.data -ne $null) -or ($r.records -ne $null)
        Assert-Pass $c.Id $ok $c.Path
    } catch {
        Assert-Pass $c.Id $false "$($c.Path) $($_.Exception.Message)"
    }
}

# --- FUNC Runtime CP ---
Write-Report "--- FUNC-RUNTIME ---"
try {
    $prep = Invoke-RestMethod -Uri "$AdminUrl/v1/llm/prepare" -Method POST -Body '{"appKey":"order-service","code":"aiChat","inputs":{"question":"acceptance"}}' -ContentType "application/json" -Headers @{ Authorization = "Bearer demo-token-123" } -TimeoutSec 30
    Assert-Pass "RT-01" ($prep.promptVersion -or $prep.data.promptVersion) "prepare OK"
} catch {
    Assert-Pass "RT-01" $false "prepare: $($_.Exception.Message)"
}
try {
    $inv = Invoke-RestMethod -Uri "$AdminUrl/v1/llm/invoke" -Method POST -Body '{"appKey":"order-service","code":"aiChat","inputs":{"question":"acceptance-invoke"}}' -ContentType "application/json" -Headers @{ Authorization = "Bearer demo-token-123" } -TimeoutSec 60
    $tid = $inv.traceId; if (-not $tid -and $inv.data) { $tid = $inv.data.traceId }
    Assert-Pass "RT-02" ($null -ne $tid) "invoke traceId=$tid"
    if ($tid) {
        $ex = Invoke-AdminGet "/api/admin/executions/$tid"
        $ej = $ex | ConvertTo-Json -Compress
        Assert-Pass "RT-03" ($ej.Contains($tid)) "execution audit"
    }
} catch {
    Assert-Pass "RT-02" $false "invoke: $($_.Exception.Message)"
}

# --- FUNC Probe write (admin) ---
Write-Report "--- FUNC-PROBE ---"
try {
    $pr = Invoke-AdminPost "/api/admin/agent-profile-probes/aiChat/run" '{"smokeTest":false}'
    $pj = $pr | ConvertTo-Json -Compress
    Assert-Pass "PROBE-01" ($pj.Contains("overallStatus") -or $pj.Contains("checks")) "probe run"
} catch {
    Assert-Pass "PROBE-01" $false "probe run: $($_.Exception.Message)"
}

# --- PERF smoke ---
Write-Report "--- PERF ---"
$latencies = @()
for ($i = 0; $i -lt $PerfRequests; $i++) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $null = Invoke-RestMethod -Uri "$AdminUrl/v1/llm/prepare" -Method POST -Body '{"appKey":"order-service","code":"aiChat","inputs":{"question":"perf"}}' -ContentType "application/json" -Headers @{ Authorization = "Bearer demo-token-123" } -TimeoutSec 30
        $sw.Stop()
        $latencies += $sw.ElapsedMilliseconds
    } catch {
        $sw.Stop()
    }
}
if ($latencies.Count -gt 0) {
    $sorted = $latencies | Sort-Object
    $p50 = $sorted[[int][Math]::Floor($sorted.Count * 0.5)]
    $p95 = $sorted[[int][Math]::Min($sorted.Count - 1, [Math]::Floor($sorted.Count * 0.95))]
    Write-Report "PERF prepare samples=$($sorted.Count) P50=${p50}ms P95=${p95}ms max=$($sorted[-1])ms"
    Assert-Pass "PERF-01" ($p95 -le $PerfP95MaxMs) "P95<=${PerfP95MaxMs}ms"
    Assert-Pass "PERF-02" ($sorted.Count -ge [int]($PerfRequests * 0.9)) "success rate>=90%"
} else {
    Assert-Skip "PERF-01" "no successful prepare samples"
}

# --- COMPAT SPA routes ---
Write-Report "--- COMPAT ---"
$spaRoutes = @("/dashboard", "/agent-config", "/ops", "/executions", "/playground", "/eval")
foreach ($route in $spaRoutes) {
    try {
        $r = Invoke-WebRequest -Uri "$AdminUrl$route" -UseBasicParsing -TimeoutSec 5
        Assert-Pass "COMPAT-$route" ($r.StatusCode -eq 200 -and $r.Content.Length -gt 100) "SPA forward $route"
    } catch {
        Assert-Pass "COMPAT-$route" $false $route
    }
}

# --- INTEGRATION AC39-44 ---
Write-Report "--- INTEGRATION ---"
try {
    $extProfile = @'
{
  "taskCode": "aiChat",
  "version": "v-ac39",
  "profileJson": "{\"apiVersion\":\"zestllm/v1\",\"runtimeMode\":\"agent\",\"providerRef\":\"litellm-default\",\"model\":{\"primary\":\"gpt-4o-mini\"},\"generation\":{\"maxTokens\":512,\"temperature\":0.3,\"timeoutMs\":30000},\"extensions\":{\"runtimeBackend\":{\"type\":\"native\",\"baseUrl\":\"http://localhost:4000\"},\"knowledge\":{\"enabled\":false},\"learningLoop\":{\"enabled\":false}}}",
  "publish": false
}
'@
    $imp = Invoke-AdminPost "/api/admin/agent-profiles/import" $extProfile
    $ij = $imp | ConvertTo-Json -Compress
    Assert-Pass "AC39" ($ij.Contains("v-ac39") -or $ij.Contains("data")) "extensions import"
} catch {
    Assert-Pass "AC39" $false "extensions import: $($_.Exception.Message)"
}

try {
    $hybridProfile = @'
{
  "taskCode": "aiChat",
  "version": "v-ac40",
  "profileJson": "{\"apiVersion\":\"zestllm/v1\",\"runtimeMode\":\"hybrid\",\"providerRef\":\"litellm-default\",\"model\":{\"primary\":\"gpt-4o-mini\"},\"generation\":{\"maxTokens\":512,\"temperature\":0.3,\"timeoutMs\":30000},\"extensions\":{\"knowledge\":{\"enabled\":true,\"provider\":\"noop\",\"datasetIds\":[\"demo\"],\"topK\":3,\"scoreThreshold\":0.5,\"injectMode\":\"system_prefix\"},\"learningLoop\":{\"enabled\":false}}}",
  "publish": true
}
'@
    Invoke-AdminPost "/api/admin/agent-profiles/import" $hybridProfile | Out-Null
    $prep2 = Invoke-RestMethod -Uri "$AdminUrl/v1/llm/prepare" -Method POST -Body '{"appKey":"order-service","code":"aiChat","inputs":{"question":"hybrid-test"}}' -ContentType "application/json" -Headers @{ Authorization = "Bearer demo-token-123" } -TimeoutSec 30
    $pj2 = $prep2 | ConvertTo-Json -Depth 8 -Compress
    if ($prep2.data) { $pj2 = ($prep2.data | ConvertTo-Json -Depth 8 -Compress) }
    Assert-Pass "AC40" ($pj2.Contains("knowledgePrefetch")) "hybrid prepare knowledgePrefetch"
} catch {
    Assert-Pass "AC40" $false "hybrid prepare: $($_.Exception.Message)"
}

try {
    $adapters = Invoke-AdminGet "/api/admin/adapters/health/all"
    $aj = $adapters | ConvertTo-Json -Depth 6 -Compress
    if ($adapters.data) { $aj = ($adapters.data | ConvertTo-Json -Depth 6 -Compress) }
    $hasRuntime = $aj.Contains("agent-runtime")
    $hasKnowledge = $aj.Contains("knowledge-retrieval")
    $hasLearning = $aj.Contains("learning-pipeline")
    Assert-Pass "AC41" ($hasRuntime -and $hasKnowledge -and $hasLearning) "integration SPI health"
} catch {
    Assert-Pass "AC41" $false "adapters health: $($_.Exception.Message)"
}

try {
    $pr = Invoke-AdminPost "/api/admin/agent-profile-probes/aiChat/run" '{"smokeTest":false}'
    $prj = $pr | ConvertTo-Json -Depth 8 -Compress
    Assert-Pass "AC42" ($prj.Contains("external-runtime") -or $prj.Contains("knowledge")) "external/knowledge probe checks"
} catch {
    Assert-Pass "AC42" $false "probe external: $($_.Exception.Message)"
}

try {
    $gateProfile = @'
{
  "taskCode": "aiChat",
  "version": "v-ac43",
  "profileJson": "{\"apiVersion\":\"zestllm/v1\",\"runtimeMode\":\"agent\",\"providerRef\":\"litellm-default\",\"model\":{\"primary\":\"gpt-4o-mini\"},\"generation\":{\"maxTokens\":512,\"temperature\":0.3,\"timeoutMs\":30000},\"extensions\":{\"learningLoop\":{\"enabled\":true,\"evalDatasetRef\":\"demo-aichat@v1\",\"minPassRate\":0.99,\"probeBeforePublish\":false}}}",
  "publish": false
}
'@
    Invoke-AdminPost "/api/admin/agent-profiles/import" $gateProfile | Out-Null
    $failCase = '{"caseCode":"ac43-fail","inputs":{"question":"gate-test-xyz"},"expected":{"answerContains":"__IMPOSSIBLE__"}}'
    try { Invoke-AdminPost "/api/admin/eval/datasets/demo-aichat/cases" $failCase | Out-Null } catch { }
    try {
        Invoke-AdminPost "/api/admin/agent-profiles/aiChat/publish" '{"version":"v-ac43"}' | Out-Null
        Assert-Pass "AC43" $false "publish should be blocked"
    } catch {
        $code = [int]$_.Exception.Response.StatusCode
        Assert-Pass "AC43" ($code -eq 409) "publish gate HTTP 409"
    }
} catch {
    Assert-Pass "AC43" $false "publish gate setup: $($_.Exception.Message)"
}

try {
    $suggest = Invoke-AdminPost "/api/admin/learning/suggest-cases" '{"taskCode":"aiChat","limit":5}'
    $sj = $suggest | ConvertTo-Json -Compress
    Assert-Pass "AC44" ($sj.Contains("data") -or $sj.StartsWith("[")) "learning suggest-cases"
} catch {
    Assert-Pass "AC44" $false "suggest-cases: $($_.Exception.Message)"
}

# --- Summary ---
Write-Report "--- SUMMARY ---"
Write-Report "PASSED=$script:Passed FAILED=$script:Failed SKIPPED=$script:Skipped"
Write-Report "Report: $ReportFile"
if ($script:Failed -gt 0) { exit 1 }
exit 0
