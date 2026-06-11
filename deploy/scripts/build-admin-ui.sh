#!/usr/bin/.env bash
# 构建 Admin UI 并复制到 Spring Boot static 目录
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
UI="$ROOT/zest-llm-admin-ui"
STATIC="$ROOT/zest-llm-admin/src/main/resources/static"

echo "== npm ci + build =="
cd "$UI"
npm ci
npm run build

echo "== copy dist -> static =="
rm -rf "$STATIC"/*
cp -r dist/* "$STATIC"/

echo "Done. Restart Admin to serve updated UI from :8088"
