#!/usr/bin/env bash
# Downloads the MapLibre Native iOS xcframework that the Kotlin Toolchain can't
# provision itself (no CocoaPods/SPM support yet). Run once after cloning.
# Version must match what maplibre-compose expects (see its iOS docs).
set -euo pipefail

VERSION="6.25.1"
DIR="$(cd "$(dirname "$0")/.." && pwd)/ios-app/Frameworks"

if [ -d "$DIR/MapLibre.xcframework" ]; then
  echo "MapLibre.xcframework already present in $DIR"
  exit 0
fi

mkdir -p "$DIR"
cd "$DIR"
echo "Downloading MapLibre $VERSION..."
curl -sL "https://github.com/maplibre/maplibre-native/releases/download/ios-v${VERSION}/MapLibre.dynamic.xcframework.zip" -o MapLibre.zip
unzip -q -o MapLibre.zip
rm MapLibre.zip
echo "Done: $DIR/MapLibre.xcframework"
echo "NOTE: the -F linker paths in shared/module.yaml and ios-app/module.yaml are"
echo "absolute — update them if your checkout is not at ~/Projects/flighty-cmp."
