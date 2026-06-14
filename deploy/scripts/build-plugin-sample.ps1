param(
    [string]$PluginModule = "knowledge-echo-sample",
    [string]$TargetDir = ""
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$pluginsRoot = Join-Path $root "plugins"
if ([string]::IsNullOrWhiteSpace($TargetDir)) {
    $TargetDir = Join-Path $root "deploy\plugins"
}

Write-Host "Building plugin module: $PluginModule"
Push-Location $pluginsRoot
try {
    mvn -q -f pom.xml -pl $PluginModule package
    $jar = Get-ChildItem -Path (Join-Path $pluginsRoot "$PluginModule\target") -Filter "*.jar" |
        Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } |
        Select-Object -First 1
    if (-not $jar) {
        throw "Plugin JAR not found under $PluginModule/target"
    }
    New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null
    Copy-Item $jar.FullName (Join-Path $TargetDir $jar.Name) -Force
    Write-Host "Copied $($jar.Name) -> $TargetDir"
    Write-Host "Set zest.llm.plugins.external-dir=$TargetDir and adapters.knowledge-retrieval=echo-kb, then restart Admin"
}
finally {
    Pop-Location
}
