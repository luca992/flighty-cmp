#!/bin/bash
# Records an iOS-simulator walkthrough of either iOS app (compose | glass).
#
# Prereqs: RELEASE builds installed on the sim (debug K/N binaries are far too
# slow to demo): `./kotlin run -m ios-app[-glass] --platform=iosSimulatorArm64
# --variant=release --device-id=$UDID`, plus idb (`brew install idb-companion`).
#
# Coordinates are POINTS on an iPhone 17 Pro Max (440x956). idb swipes need
# --delta 3: the default emits sparse touch points and scrolls look robotic.
#
# Usage: capture-scripts/capture-ios.sh <sim-udid> compose|glass <out.mp4>
set -euo pipefail
UD=${1:?sim udid}
VARIANT=${2:?compose|glass}
OUT=${3:-ios-$VARIANT.mp4}
TMP=$(mktemp -d)

if [ "$VARIANT" = "glass" ]; then
  BUNDLE=com.flighty-cmp.glass
  TAB_FRIENDS="184 901"; TAB_PASSPORT="288 901"; TAB_SEARCH="386 901"
else
  BUNDLE=com.flighty-cmp.app
  TAB_FRIENDS="199 880"; TAB_PASSPORT="267 880"; TAB_SEARCH="327 875"
fi

xcrun simctl status_bar "$UD" override --time 9:41 --batteryLevel 100 \
  --cellularBars 4 --dataNetwork wifi --wifiBars 3
# warm launch first: a cold first run after install stutters
xcrun simctl terminate "$UD" "$BUNDLE" 2>/dev/null || true
xcrun simctl launch "$UD" "$BUNDLE" >/dev/null; sleep 5
xcrun simctl terminate "$UD" "$BUNDLE" 2>/dev/null || true
sleep 2

xcrun simctl io "$UD" recordVideo --codec h264 --force "$TMP/raw.mov" &
REC=$!
sleep 1.5
xcrun simctl launch "$UD" "$BUNDLE"
sleep 2.6
idb ui tap --udid "$UD" 220 431; sleep 1.7                                  # detail
idb ui swipe --udid "$UD" 220 760 220 380 --duration 0.14 --delta 3; sleep 0.8   # flick to bottom
idb ui swipe --udid "$UD" 220 760 220 380 --duration 0.14 --delta 3; sleep 1.0
idb ui tap --udid "$UD" 118 884; sleep 1.2                                  # ... flight menu
idb ui tap --udid "$UD" 350 150; sleep 0.3                                  # dismiss
idb ui swipe --udid "$UD" 220 380 220 760 --duration 0.14 --delta 3; sleep 0.7   # flick back up
idb ui swipe --udid "$UD" 220 380 220 760 --duration 0.14 --delta 3; sleep 0.7
idb ui swipe --udid "$UD" 220 477 220 875 --duration 0.3 --delta 3; sleep 0.4    # reveal map
idb ui tap --udid "$UD" 409 467; sleep 1.4                                  # X -> home
idb ui tap --udid "$UD" $TAB_FRIENDS; sleep 1.5
idb ui tap --udid "$UD" $TAB_PASSPORT; sleep 1.5
idb ui tap --udid "$UD" $TAB_SEARCH; sleep 2.0                              # Add Flight
idb ui swipe --udid "$UD" 220 200 220 900 --duration 0.4 --delta 3; sleep 1.2    # dismiss sheet
idb ui tap --udid "$UD" 401 365; sleep 1.9                                  # profile menu
idb ui tap --udid "$UD" 150 600; sleep 1.6                                  # dismiss
kill -INT $REC
wait $REC 2>/dev/null || true
sleep 1

# Trim the springboard lead-in; 60fps CFR keeps every rendered frame.
ffmpeg -y -v error -ss 2.4 -i "$TMP/raw.mov" -fps_mode cfr -r 60 \
  -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p "$OUT"
echo "wrote $OUT"
# NOTE: if recordVideo says "Host recording is already in progress", a prior
# recording is orphaned inside SimRender — `xcrun simctl shutdown/boot` the sim
# (do NOT kill SimRender; that shuts the whole simulator down).
