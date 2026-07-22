#!/bin/bash
# Records the desktop (JVM) walkthrough by driving the app window with
# synthetic mouse events (Quartz, via deskdrive.py).
#
# Prereqs:
#   - `pip install pyobjc pillow`
#   - the Flighty window on the ACTIVE macOS Space, hands off mouse/keyboard
#     for the ~30s take (events go through the real cursor)
#   - terminal has Accessibility + Screen Recording permissions
#
# The script RESTARTS the app first: a fresh launch is the only reliable
# reset (blind clicks against an assumed UI state are how takes die), and it
# guarantees clean mock data. Targets that vary with sheet state (detail
# close X, avatar) are located from live pixels mid-take — each find costs
# ~0.5s, which is why the choreography uses them only where a blind
# coordinate has actually been seen to miss.
#
# Usage: capture-scripts/capture-desktop.sh <out.mp4>
set -euo pipefail
OUT=${1:-desktop.mp4}
HERE=$(cd "$(dirname "$0")" && pwd)
REPO=$(cd "$HERE/.." && pwd)
TMP=$(mktemp -d)

pkill -f MainKt 2>/dev/null || true
sleep 2
(cd "$REPO" && ./kotlin run -m jvm-app > "$TMP/app.log" 2>&1 &)
sleep 24

python3 - "$HERE" << 'EOF'
import sys, time
sys.path.insert(0, sys.argv[1])
import Quartz
from AppKit import NSRunningApplication, NSApplicationActivateIgnoringOtherApps
from deskdrive import App
app = App()
wins = Quartz.CGWindowListCopyWindowInfo(Quartz.kCGWindowListOptionOnScreenOnly, Quartz.kCGNullWindowID)
pid = next(w['kCGWindowOwnerPID'] for w in wins if w.get('kCGWindowOwnerName') == 'MainKt')
NSRunningApplication.runningApplicationWithProcessIdentifier_(pid).activateWithOptions_(NSApplicationActivateIgnoringOtherApps)
time.sleep(1.0)
EOF

WINID=$(python3 -c "
import sys; sys.path.insert(0, '$HERE')
from deskdrive import bounds
print(bounds()[0])")

screencapture -v -V 32 -l"$WINID" "$TMP/raw.mov" &
REC=$!
sleep 1.2

python3 - "$HERE" << 'EOF'
import sys, time
sys.path.insert(0, sys.argv[1])
from deskdrive import App
app = App()

def find_avatar():
    im = app.img().convert('RGB')
    for cy in range(80, 480, 3):
        for cx in range(355, 412, 3):
            r, g, b = app.probe(im, cx, cy)
            if 90 < r < 150 and g < 120 and b > 200:
                return cx - 8, cy
    return 385, 330

time.sleep(2.2)
app.click(215, 395); time.sleep(1.6)                   # open detail
app.glide(215, 500, -1250, 0.85); time.sleep(0.35)     # trackpad-style scroll to bottom
app.glide(215, 500, -1250, 0.85); time.sleep(0.5)
app.click(116, 838); time.sleep(1.1)                   # flight menu from the ... pill
app.click(392, 300); time.sleep(0.2)                   # dismiss
app.glide(215, 500, 1250, 0.7); time.sleep(0.25)       # back to top
app.glide(215, 500, 1250, 0.7); time.sleep(0.35)
app.drag(215, 120, 215, 500, 0.45); time.sleep(0.3)    # pull sheet to the small state
xy = app.find_close_x() or 440
app.click(390, xy); time.sleep(1.35)                   # X -> home (located live)
app.click(192, 816); time.sleep(1.35)                  # Friends
app.click(259, 816); time.sleep(1.35)                  # Passport
app.click(318, 816); time.sleep(1.8)                   # search -> Add Flight
app.click(390, 76); time.sleep(1.0)                    # its X (fullscreen sheet: stable)
ax, ay = find_avatar()
app.click(ax, ay); time.sleep(1.7)                     # profile menu
app.click(140, 500); time.sleep(1.2)
EOF

wait $REC 2>/dev/null || true
ffmpeg -y -v error -i "$TMP/raw.mov" -fps_mode cfr -r 60 \
  -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p "$OUT"
echo "wrote $OUT"
