#!/bin/bash
# Rebuilds the release wasm distribution into docs/ for GitHub Pages.
#
# GitHub Pages serves the repo's docs/ folder at
# https://luca992.github.io/flighty-cmp/ — the checked-in dist IS the
# deployment; no external hosting. Re-run this and commit whenever the app
# changes. (Resources are copied in manually: the 0.12-dev toolchain doesn't
# package composeResources for wasm — see scripts/serve-web.sh.)
set -euo pipefail
cd "$(dirname "$0")/.."
./kotlin build -m web-app --variant=release
rm -rf docs
mkdir -p docs
cp -R build/tasks/_web-app_buildWasmJsAppWasmJsRelease/. docs/
cp -R build/artifacts/JvmResourcesDirArtifact/sharedcommon/composeResources docs/
touch docs/.nojekyll   # Jekyll would ignore files/dirs it dislikes; serve as-is
echo "docs/ ready ($(du -sh docs | cut -f1)) — commit and push to deploy"
