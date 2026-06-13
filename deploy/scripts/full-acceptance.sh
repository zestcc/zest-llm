# 生产级全量验收入口（Linux / Docker）— 见 production-acceptance.sh
exec bash "$(dirname "$0")/production-acceptance.sh" "$@"
