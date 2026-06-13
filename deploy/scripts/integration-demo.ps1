# Windows 本地无 Docker — 仅文档入口；B 整合 Demo 请在 Linux 执行：
#
#   bash deploy/scripts/zest-stack-up.sh large
#   bash deploy/scripts/wait-stack-ready.sh
#   bash deploy/scripts/integration-demo.sh
#
# 严格验收：
#   bash deploy/scripts/run-integration-acceptance.sh
#
# 详见 docs/B整合栈Demo指南.md

Write-Host "B integration demo requires Linux Docker (large + integration profile)." -ForegroundColor Yellow
Write-Host "Run: bash deploy/scripts/integration-demo.sh" -ForegroundColor Cyan
Write-Host "Doc: docs/B整合栈Demo指南.md"
