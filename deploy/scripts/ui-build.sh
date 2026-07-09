#!/usr/bin/env bash
# cat2bug-platform-ui 构建入口：兼容 Node 16–24。
# Node 17+ 的 webpack4/vue-cli 需 --openssl-legacy-provider；
# Node 20+ 禁止经 NODE_OPTIONS 传入该参数，须直接传给 node 可执行文件。
#
# 用法:
#   ./deploy/scripts/ui-build.sh [mode]
#   mode: production（默认）| embedded | cloud | staging
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
UI_DIR="$ROOT/cat2bug-platform-ui"
MODE="${1:-production}"

cd "$UI_DIR"

# 使用真实的 JS 入口
VUE_CLI_SERVICE_JS="$UI_DIR/node_modules/@vue/cli-service/bin/vue-cli-service.js"
if [[ ! -f "$VUE_CLI_SERVICE_JS" ]]; then
  echo "[ERROR] 未找到 vue-cli-service.js，请确认 @vue/cli-service 已安装" >&2
  exit 1
fi

ARGS=("$VUE_CLI_SERVICE_JS" build)
if [[ "$MODE" != "production" ]]; then
  ARGS+=(--mode "$MODE")
fi

NODE_MAJOR="$(node -p 'parseInt(process.versions.node.split(".")[0], 10)')"
if [[ "$NODE_MAJOR" -ge 17 ]]; then
  exec node --openssl-legacy-provider "${ARGS[@]}"
else
  exec node "${ARGS[@]}"
fi