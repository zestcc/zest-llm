#Requires -Version 5.1
# 与 deploy/scripts 下其它脚本同目录；转发到仓库根 scripts/
& "$PSScriptRoot\..\..\scripts\import-zestllm-quality-profiles.ps1" @args
exit $LASTEXITCODE
