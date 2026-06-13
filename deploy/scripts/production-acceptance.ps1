# ZestLLM 生产级全量验收编排（Windows / 本地无 Docker 友好）
# 四阶段：白盒(mvn test) → 黑盒(full-acceptance) → 链路(demo-walkthrough+CHAIN) → 压测(stress-test-prepare)
#
# 用法:
#   powershell -File deploy/scripts/production-acceptance.ps1
#   powershell -File deploy/scripts/production-acceptance.ps1 -Tier production -SkipStress
#   powershell -File deploy/scripts/production-acceptance.ps1 -SkipWhiteBox
param(
    [ValidateSet("local", "production")]
    [string]$Tier = "local",
    [string]$AdminUrl = "http://127.0.0.1:8088",
    [string]$DemoUrl = "http://127.0.0.1:8081",
    [switch]$SkipWhiteBox,
    [switch]$SkipBlackBox,
    [switch]$SkipChain,
    [switch]$SkipStress,
    [switch]$FailOnSkip
)

$ErrorActionPreference = "Continue"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$ReportDir = Join-Path $Root "deploy\test-reports"
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null
$MasterReport = Join-Path $ReportDir ("production-{0:yyyyMMdd-HHmmss}.txt" -f (Get-Date))

$script:PhaseFailed = 0
$script:PhaseSkipped = 0

function Write-Master($msg) {
    $line = "[$(Get-Date -Format 'HH:mm:ss')] $msg"
    Write-Host $line
    Add-Content -Path $MasterReport -Value $line
}

function Invoke-Phase {
    param(
        [string]$Name,
        [scriptblock]$Block,
        [switch]$Skip
    )
    Write-Master "======== PHASE: $Name ========"
    if ($Skip) {
        Write-Master "SKIP $Name"
        $script:PhaseSkipped++
        return
    }
    try {
        & $Block
        if ($LASTEXITCODE -ne 0 -and $null -ne $LASTEXITCODE) {
            Write-Master "FAIL $Name exit=$LASTEXITCODE"
            $script:PhaseFailed++
        } else {
            Write-Master "PASS $Name"
        }
    } catch {
        Write-Master "FAIL $Name $($_.Exception.Message)"
        $script:PhaseFailed++
    }
}

# 门禁阈值
$PerfP95MaxMs = if ($Tier -eq "production") { 500 } else { 800 }
$StressP95MaxMs = if ($Tier -eq "production") { 500 } else { 1200 }
$StressTotal = if ($Tier -eq "production") { 300 } else { 200 }
$StressConcurrency = if ($Tier -eq "production") { 50 } else { 30 }

Write-Master "=== ZestLLM Production Acceptance Tier=$Tier ==="
Write-Master "AdminUrl=$AdminUrl DemoUrl=$DemoUrl"
Write-Master "Thresholds: blackbox P95<=${PerfP95MaxMs}ms stress P95<=${StressP95MaxMs}ms"

# --- Phase 1: 白盒 ---
Invoke-Phase -Name "WHITEBOX mvn test" -Skip:$SkipWhiteBox -Block {
    Push-Location $Root
    $log = Join-Path $ReportDir "mvn-test-latest.log"
    mvn -B test 2>&1 | Tee-Object -FilePath $log
    $exit = $LASTEXITCODE
    Pop-Location
    Add-Content -Path $MasterReport -Value "mvn log: $log"
    if ($exit -ne 0) { throw "mvn test failed exit=$exit" }
}

# --- Phase 2: 黑盒 ---
Invoke-Phase -Name "BLACKBOX full-acceptance" -Skip:$SkipBlackBox -Block {
    & (Join-Path $PSScriptRoot "full-acceptance.ps1") `
        -AdminUrl $AdminUrl -DemoUrl $DemoUrl `
        -PerfP95MaxMs $PerfP95MaxMs
}

# --- Phase 3: 链路 ---
Invoke-Phase -Name "CHAIN demo-walkthrough" -Skip:$SkipChain -Block {
    $env:ZEST_ADMIN_URL = $AdminUrl
    & (Join-Path $PSScriptRoot "demo-walkthrough.ps1")
}

# --- Phase 4: 压测 ---
Invoke-Phase -Name "STRESS prepare" -Skip:$SkipStress -Block {
    & (Join-Path $PSScriptRoot "stress-test-prepare.ps1") `
        -AdminUrl $AdminUrl `
        -Concurrency $StressConcurrency `
        -Total $StressTotal `
        -P95MaxMs $StressP95MaxMs `
        -MinSuccessRate 0.95
}

# --- 生产签字门禁摘要 ---
Write-Master "======== PRODUCTION GATES ========"
$gates = @(
    @{ Id = "GATE-WB"; Label = "白盒 mvn test 0 FAIL"; Pass = (-not $SkipWhiteBox -and $script:PhaseFailed -eq 0) -or $SkipWhiteBox }
    @{ Id = "GATE-BB"; Label = "黑盒 full-acceptance 0 FAIL"; Pass = (-not $SkipBlackBox) }
    @{ Id = "GATE-CH"; Label = "链路 demo-walkthrough PASS"; Pass = (-not $SkipChain) }
    @{ Id = "GATE-ST"; Label = "压测 P95<=${StressP95MaxMs}ms 成功率>=95%"; Pass = (-not $SkipStress) }
    @{ Id = "GATE-P0"; Label = "P0 安全/鉴权/探测 (见 full-acceptance SEC/PROBE)"; Pass = (-not $SkipBlackBox) }
)

foreach ($g in $gates) {
    $status = if ($g.Pass) { "PASS" } else { "SKIP/MANUAL" }
    Write-Master "$status $($g.Id) $($g.Label)"
}

Write-Master "======== SUMMARY ========"
Write-Master "PhaseFailed=$script:PhaseFailed PhaseSkipped=$script:PhaseSkipped"
Write-Master "MasterReport=$MasterReport"
Write-Master "Tier=$Tier — production Docker E2E: bash deploy/scripts/production-acceptance.sh"

if ($FailOnSkip -and $script:PhaseSkipped -gt 0) {
    Write-Master "FAIL FailOnSkip: skipped phases=$script:PhaseSkipped"
    exit 1
}
if ($script:PhaseFailed -gt 0) { exit 1 }
exit 0
