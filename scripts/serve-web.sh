#!/bin/bash
# Builds the wasmJs web app and serves it locally.
#
# Needed because the toolchain (0.12-dev) does not yet package or serve
# Compose resources for wasm targets: neither the built dist nor
# `./kotlin run -m web-app`'s dev server contains composeResources/, so
# images 404 and e.g. the globe texture never loads. This script copies the
# merged resources into the dist before serving.
#
# Usage: scripts/serve-web.sh [port]   (default 8080)
set -euo pipefail
PORT=${1:-8080}
cd "$(dirname "$0")/.."
./kotlin build -m web-app
DIST=build/tasks/_web-app_buildWasmJsAppWasmJsDebug
cp -R build/artifacts/JvmResourcesDirArtifact/sharedcommon/composeResources "$DIST/"
echo "Serving http://localhost:$PORT"
cd "$DIST" && exec python3 -m http.server "$PORT"
