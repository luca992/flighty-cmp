#!/bin/bash
# Assembles demo-captures/composite.mp4 from the four platform captures.
#
# Layout: 1920x1080 light canvas, rendered Helvetica Neue Bold header, four
# rounded-corner panels at TRUE aspect ratios (phones 405x880 from 1320x2868,
# desktop 420x880 from its 860x1802 window crop).
#
# Sync model — all panels beat-aligned to the glass panel:
#   * every -ss below trims so the detail-OPEN lands at t=1.6s
#   * pre-close freezes (loop filter) / speed windows (select filter) make the
#     detail-CLOSE land at the same instant everywhere
# The constants were derived by measuring beats with frame-diff scans (open =
# first sustained 30fps motion after launch; close = first frame the home
# screen is back). RE-MEASURE AND RE-TUNE whenever any capture is re-recorded:
# verify by extracting composite frames around t=1.7 (all mid slide-in) and
# t=11.4-11.8 (all back on My Flights).
#
# Usage: capture-scripts/build-composite.sh   (run from repo root)
set -euo pipefail
cd "$(dirname "$0")/../demo-captures"

# ---- rounded-corner masks + header text (PIL: ffmpeg drawtext can't do
# proper font weights, and DropdownMenu-style ttc face selection needs index) --
python3 << 'EOF'
from PIL import Image, ImageDraw, ImageFont
for name, w, h, r in [('/tmp/mask_phone.png', 405, 880, 38), ('/tmp/mask_desk.png', 420, 880, 16)]:
    m = Image.new('L', (w, h), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, w-1, h-1], radius=r, fill=255)
    m.save(name)
W, H = 1920, 140
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
title_f = ImageFont.truetype('/System/Library/Fonts/HelveticaNeue.ttc', 42, index=1)   # Bold
label_f = ImageFont.truetype('/System/Library/Fonts/HelveticaNeue.ttc', 25, index=10)  # Medium
title = 'Flighty Clone: Kotlin Compose Multiplatform'
d.text(((W - d.textlength(title, font=title_f)) / 2, 20), title, font=title_f, fill=(17, 20, 28, 255))
for text, cx in [('iOS · Native Liquid Glass + CMP', 259), ('iOS · CMP', 721), ('Android', 1183), ('Desktop · JVM', 1653)]:
    d.text((cx - d.textlength(text, font=label_f) / 2, 94), text, font=label_f, fill=(88, 94, 106, 255))
img.save('/tmp/header_overlay.png')
EOF

# ---- retimed intermediates (see sync model above) ----
# glass: the reference panel; open at 2.233s in source
ffmpeg -y -v error -ss 0.633 -i ios-glass.mp4 -vf "fps=60" -t 26 \
  -c:v libx264 -preset fast -crf 15 -pix_fmt yuv420p /tmp/glass_rt.mp4
# compose: opens 2.067s; closes early -> hold 18 frames just before its close
ffmpeg -y -v error -ss 0.467 -i ios-compose.mp4 \
  -vf "fps=60,loop=loop=18:size=1:start=635,setpts=N/60/TB" -t 26 \
  -c:v libx264 -preset fast -crf 15 -pix_fmt yuv420p /tmp/compose_rt.mp4
# android: opens 2.4s; runs 0.885x-fast overall + 119-frame pre-close hold
ffmpeg -y -v error -ss 0.592 -i android.mp4 \
  -vf "fps=60,loop=loop=119:size=1:start=625,setpts=N/60/TB,setpts=PTS*0.885,fps=60" -t 26 \
  -c:v libx264 -preset fast -crf 15 -pix_fmt yuv420p /tmp/android_rt.mp4
# desktop: opens 3.933s; crop removes the window-capture shadow padding
# (asymmetric: 112px sides / 76px top at 2x); return leg (scroll-up, sheet
# pull-down, X) plays 1.4x by dropping 2 of every 7 frames in that window
ffmpeg -y -v error -ss 2.333 -i desktop.mp4 \
  -vf "crop=860:1802:112:76,fps=60,select='not(between(t,8.4,12.6)*lt(mod(n,7),2))',setpts=N/60/TB" -t 26 \
  -c:v libx264 -preset fast -crf 15 -pix_fmt yuv420p /tmp/desktop_rt.mp4

# ---- assembly ----
cat > /tmp/composite.filter << 'FILTER'
color=c=0xF2F2F5:s=1920x1080:r=60:d=25.5[bg0];
[6:v]format=rgba[hdr];
[bg0][hdr]overlay=0:0[bg];
[4:v]format=gray,scale=405:880[mp];
[5:v]format=gray,scale=420:880[md];
[0:v]scale=405:880,setsar=1,tpad=stop_mode=clone:stop_duration=8[g0];
[1:v]scale=405:880,setsar=1,tpad=stop_mode=clone:stop_duration=8[g1];
[2:v]scale=405:880,setsar=1,tpad=stop_mode=clone:stop_duration=8[g2];
[3:v]scale=420:880,setsar=1,tpad=stop_mode=clone:stop_duration=8[g3];
[mp]split[mp0][mpx];[mpx]split[mp1][mp2];
[g0][mp0]alphamerge[p0];
[g1][mp1]alphamerge[p1];
[g2][mp2]alphamerge[p2];
[g3][md]alphamerge[p3];
[bg][p0]overlay=57:140[a];
[a][p1]overlay=519:140[b];
[b][p2]overlay=981:140[c];
[c][p3]overlay=1443:140[out]
FILTER
ffmpeg -y -v error \
  -i /tmp/glass_rt.mp4 -i /tmp/compose_rt.mp4 -i /tmp/android_rt.mp4 -i /tmp/desktop_rt.mp4 \
  -loop 1 -i /tmp/mask_phone.png -loop 1 -i /tmp/mask_desk.png -loop 1 -i /tmp/header_overlay.png \
  -filter_complex_script /tmp/composite.filter \
  -map "[out]" -t 25.5 -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p composite.mp4

# poster + README-embeddable GIF (GitHub won't render committed mp4s inline)
ffmpeg -y -v error -ss 7 -i composite.mp4 -frames:v 1 poster.png
ffmpeg -y -v error -i composite.mp4 \
  -vf "fps=12,scale=840:-1:flags=lanczos,split[s0][s1];[s0]palettegen=max_colors=128[p];[s1][p]paletteuse=dither=bayer:bayer_scale=4" \
  composite.gif
echo "wrote composite.mp4, poster.png, composite.gif"
