#!/usr/bin/env bash
# Downloads the MapLibre Native iOS xcframework that the Kotlin Toolchain can't
# provision itself (no CocoaPods/SPM support yet), and rewrites the absolute
# -F linker paths in the module.yaml files to this checkout's location.
# Run once after cloning. Version must match what maplibre-compose expects.
set -euo pipefail

VERSION="6.25.1"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIR="$ROOT/ios-app/Frameworks"

if [ -d "$DIR/MapLibre.xcframework" ]; then
  echo "MapLibre.xcframework already present in $DIR"
else
  mkdir -p "$DIR"
  cd "$DIR"
  echo "Downloading MapLibre $VERSION..."
  curl -sL "https://github.com/maplibre/maplibre-native/releases/download/ios-v${VERSION}/MapLibre.dynamic.xcframework.zip" -o MapLibre.zip
  unzip -q -o MapLibre.zip
  rm MapLibre.zip
  echo "Downloaded: $DIR/MapLibre.xcframework"
fi

# The Kotlin/Native linker needs absolute -F paths (relative paths resolve
# against varying working directories across link tasks). Point them at this
# checkout, wherever it lives.
for f in "$ROOT/shared/module.yaml" "$ROOT/ios-app/module.yaml"; do
  sed -i.bak "s|-F.*/ios-app/Frameworks/MapLibre.xcframework|-F$ROOT/ios-app/Frameworks/MapLibre.xcframework|" "$f"
  rm -f "$f.bak"
done
echo "Linker paths in shared/module.yaml and ios-app/module.yaml now point at:"
echo "  $ROOT/ios-app/Frameworks/MapLibre.xcframework"
