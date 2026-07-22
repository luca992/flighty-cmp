#!/usr/bin/env python3
"""Adaptive driver for the Flighty desktop window.

Every action re-queries the window bounds; verification screenshots let the
choreography confirm state instead of firing blind.
"""
import subprocess
import sys
import time

import Quartz
from PIL import Image

SHOT = '/tmp/deskdrive_shot.png'


def bounds():
    wins = Quartz.CGWindowListCopyWindowInfo(
        Quartz.kCGWindowListOptionOnScreenOnly, Quartz.kCGNullWindowID)
    for w in wins:
        if w.get('kCGWindowOwnerName') == 'MainKt' and \
                w.get('kCGWindowBounds', {}).get('Height', 0) > 300:
            b = w['kCGWindowBounds']
            return w['kCGWindowNumber'], b['X'], b['Y'], b['Width'], b['Height']
    raise SystemExit('window not on this Space')


def shot(win_id):
    subprocess.run(['screencapture', '-x', f'-l{win_id}', SHOT], check=True)
    return Image.open(SHOT)


class App:
    def refresh(self):
        self.win, self.x, self.y, self.w, self.h = bounds()
        # content area: title bar 28pt below the window top
        self.cx0, self.cy0 = self.x, self.y + 28
        self.ch = self.h - 28

    def __init__(self):
        self.refresh()

    def to_screen(self, cx, cy):
        return (self.cx0 + cx, self.cy0 + cy)

    def img(self):
        im = shot(self.win)
        # map content-pt -> image px: image includes shadow padding
        self.sf = im.width / (self.w + 112)  # 56pt shadow each side
        self.pad = 56
        return im

    PAD_TOP = 38

    def probe(self, im, cx, cy):
        px = int((cx + self.pad) * self.sf)
        py = int((cy + self.PAD_TOP + 28) * self.sf)
        return im.convert('RGB').getpixel((px, py))

    def _post(self, etype, p, clicks=0):
        e = Quartz.CGEventCreateMouseEvent(None, etype, p, Quartz.kCGMouseButtonLeft)
        if clicks:
            Quartz.CGEventSetIntegerValueField(e, Quartz.kCGMouseEventClickState, clicks)
        Quartz.CGEventPost(Quartz.kCGHIDEventTap, e)

    def move(self, cx, cy):
        self._post(Quartz.kCGEventMouseMoved, self.to_screen(cx, cy))

    def human_move(self, cx, cy, dur=None):
        """Glide the cursor from wherever it is to the target along a gentle
        arc with ease-in-out, like a hand moving a mouse."""
        cur = Quartz.CGEventGetLocation(Quartz.CGEventCreate(None))
        tx, ty = self.to_screen(cx, cy)
        dx, dy = tx - cur.x, ty - cur.y
        dist = (dx * dx + dy * dy) ** 0.5
        if dist < 3:
            return
        if dur is None:
            dur = min(0.55, 0.18 + dist / 1800.0)
        # perpendicular bow for a natural arc
        bow = min(40.0, dist * 0.12)
        px, py = -dy / dist * bow, dx / dist * bow
        steps = max(int(dur * 120), 10)
        t0 = time.time()
        for i in range(1, steps + 1):
            f = i / steps
            ease = f * f * (3 - 2 * f)  # smoothstep
            arc = 4 * ease * (1 - ease)  # 0 at ends, 1 mid
            x = cur.x + dx * ease + px * arc
            y = cur.y + dy * ease + py * arc
            self._post(Quartz.kCGEventMouseMoved, (x, y))
            d = t0 + dur * f - time.time()
            if d > 0:
                time.sleep(d)

    def click(self, cx, cy):
        self.refresh()
        self.human_move(cx, cy)
        p = self.to_screen(cx, cy)
        time.sleep(0.10)
        self._post(Quartz.kCGEventLeftMouseDown, p, 1)
        time.sleep(0.08)
        self._post(Quartz.kCGEventLeftMouseUp, p, 1)

    def drag(self, cx1, cy1, cx2, cy2, dur=0.5):
        self.refresh()
        self.human_move(cx1, cy1)
        p1, p2 = self.to_screen(cx1, cy1), self.to_screen(cx2, cy2)
        time.sleep(0.08)
        self._post(Quartz.kCGEventLeftMouseDown, p1, 1)
        steps = max(int(dur * 120), 14)
        t0 = time.time()
        for i in range(1, steps + 1):
            f = i / steps
            ease = 1 - (1 - f) ** 2.0
            self._post(Quartz.kCGEventLeftMouseDragged,
                       (p1[0] + (p2[0] - p1[0]) * ease, p1[1] + (p2[1] - p1[1]) * ease), 1)
            d = t0 + dur * f - time.time()
            if d > 0:
                time.sleep(d)
        self._post(Quartz.kCGEventLeftMouseUp, p2, 1)

    def wheel(self, cx, cy, lines, dur=0.9):
        """Eased line-unit wheel burst; negative lines scroll content down."""
        self.refresh()
        self.human_move(cx, cy)
        time.sleep(0.06)
        steps = max(int(dur * 60), 15)
        t0 = time.time()
        prev = 0.0
        for i in range(1, steps + 1):
            f = i / steps
            ease = 1 - (1 - f) ** 2.4
            cur = lines * ease
            d = cur - prev
            if abs(d) >= 1:
                di = int(d)
                prev += di
                e = Quartz.CGEventCreateScrollWheelEvent(
                    None, Quartz.kCGScrollEventUnitLine, 1, di)
                Quartz.CGEventPost(Quartz.kCGHIDEventTap, e)
            dd = t0 + dur * f - time.time()
            if dd > 0:
                time.sleep(dd)

    def glide(self, cx, cy, px, dur=1.3):
        """Trackpad-like continuous pixel scroll with a fling-decay profile:
        quick ramp-up then a long momentum tail, like a finger flick."""
        self.refresh()
        self.human_move(cx, cy)
        time.sleep(0.06)
        steps = max(int(dur * 90), 30)
        t0 = time.time()
        prev = 0.0
        for i in range(1, steps + 1):
            f = i / steps
            # smooth flick: fast acceleration, exponential-style decay
            ease = 1 - (1 - f) ** 3.2
            cur = px * ease
            d = cur - prev
            di = int(d)
            if di:
                prev += di
                e = Quartz.CGEventCreateScrollWheelEvent(
                    None, Quartz.kCGScrollEventUnitPixel, 1, di)
                Quartz.CGEventSetIntegerValueField(
                    e, Quartz.kCGScrollWheelEventIsContinuous, 1)
                Quartz.CGEventPost(Quartz.kCGHIDEventTap, e)
            dd = t0 + dur * f - time.time()
            if dd > 0:
                time.sleep(dd)

    def find_close_x(self):
        """Locate the X glyph in the sheet header (below the map band)."""
        im = self.img().convert('L')
        # sheet top: first y where the center column turns light
        top = 40
        for cy in range(20, int(self.ch) - 200, 2):
            if self.probe_l(im, 215, cy) > 200:
                top = cy
                break
        best = (255, None)
        for cy in range(top + 10, top + 130, 2):
            # only consider rows that are actually ON the sheet: the area
            # left of the X circle must be light (rules out the dark map
            # band and the rounded-corner gaps)
            if self.probe_l(im, 355, cy) < 200:
                continue
            v = self.probe_l(im, 392, cy)
            if v < best[0]:
                best = (v, cy)
        return best[1]

    def probe_l(self, im_l, cx, cy):
        px = int((cx + self.pad) * self.sf)
        py = int((cy + self.PAD_TOP + 28) * self.sf)
        return im_l.getpixel((px, py))


if __name__ == '__main__':
    app = App()
    cmd = sys.argv[1]
    if cmd == 'bounds':
        print(app.win, app.x, app.y, app.w, app.h)
    elif cmd == 'click':
        app.click(float(sys.argv[2]), float(sys.argv[3]))
    elif cmd == 'wheel':
        app.wheel(215, 500, int(sys.argv[2]), float(sys.argv[3]) if len(sys.argv) > 3 else 0.9)
    elif cmd == 'drag':
        app.drag(*[float(a) for a in sys.argv[2:6]])
