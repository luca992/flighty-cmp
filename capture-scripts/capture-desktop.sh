#!/bin/bash
# Records the desktop (JVM) walkthrough by driving the app window with
# synthetic mouse events (Quartz, via deskdrive.py).
#
# Prereqs:
#   - app running: `./kotlin run -m jvm-app`
#   - `pip install pyobjc pillow`
#   - the Flighty window VISIBLE on the ACTIVE macOS Space, hands off the
#     mouse/keyboard for the ~50s take (events go through the real cursor)
#   - terminal has Accessibility + Screen Recording permissions
#
# The driver is adaptive: it re-queries window bounds before every action and
# locates the close X / sheet top / avatar from live screenshots, so window
# position and sheet state don't need to be exact.
#
# Usage: capture-scripts/capture-desktop.sh <out.mp4>
set -euo pipefail
OUT=${1:-desktop.mp4}
HERE=$(cd "$(dirname "$0")" && pwd)
TMP=$(mktemp -d)

WINID=$(python3 -c "
import sys; sys.path.insert(0, '$HERE')
from deskdrive import bounds
print(bounds()[0])")

# reset to My Flights at peek (best effort)
python3 - "$HERE" << 'EOF'
import sys, time
sys.path.insert(0, sys.argv[1])
from deskdrive import App
app = App()
xy = app.find_close_x()
if xy: app.click(390, xy); time.sleep(1.3)
app.click(120, 816); time.sleep(1.2)
app.drag(215, 200, 215, 560, 0.45); time.sleep(1.0)
EOF

screencapture -v -V 52 -l"$WINID" "$TMP/raw.mov" &
REC=$!
sleep 1.2

python3 - "$HERE" << 'EOF'
import sys, time
sys.path.insert(0, sys.argv[1])
from deskdrive import App
app = App()

def sheet_top():
    im = app.img().convert('L')
    for cy in range(10, int(app.ch) - 200, 2):
        if app.probe_l(im, 215, cy) > 200:
            return cy
    return 100

def find_avatar():
    im = app.img().convert('RGB')
    for cy in range(80, 460, 4):
        for cx in range(360, 410, 4):
            r, g, b = app.probe(im, cx, cy)
            if 90 < r < 150 and g < 120 and b > 200:
                return cx, cy
    return 390, 330

time.sleep(2.2)
app.click(215, 395); time.sleep(1.7)                    # open detail
app.glide(215, 500, -1250, 1.25); time.sleep(0.5)       # trackpad-style scroll to bottom
app.glide(215, 500, -1250, 1.25); time.sleep(0.8)
app.click(116, 838); time.sleep(1.2)                    # ... flight menu
app.click(392, 300); time.sleep(0.4)                    # dismiss
app.glide(215, 500, 1250, 1.15); time.sleep(0.4)        # back to top
app.glide(215, 500, 1250, 1.15); time.sleep(0.6)
st = sheet_top()                                        # grab the real handle
app.drag(215, st + 6, 215, st + 380, 0.55); time.sleep(0.6)   # pull sheet to peek
xy = app.find_close_x() or 448
app.click(390, xy); time.sleep(1.5)                     # close detail
app.click(192, 816); time.sleep(1.5)                    # Friends
app.click(259, 816); time.sleep(1.5)                    # Passport
app.click(318, 816); time.sleep(2.0)                    # Add Flight
axy = app.find_close_x() or 76
app.click(390, axy); time.sleep(1.2)                    # dismiss via its X
ax, ay = find_avatar()
app.click(ax, ay); time.sleep(1.9)                      # profile menu
app.click(140, 500); time.sleep(1.5)
EOF

wait $REC 2>/dev/null || true
ffmpeg -y -v error -i "$TMP/raw.mov" -fps_mode cfr -r 60 \
  -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p "$OUT"
echo "wrote $OUT"
