# Walkthrough capture scripts

Scripted, reproducible screen captures of the same ~30s demo choreography on
every platform: home → flight detail → scroll to bottom → flight-settings
menu → scroll back → reveal map → close → Friends → Passport → Add Flight →
profile menu.

| Script | Target | Input mechanism |
| --- | --- | --- |
| `capture-android.sh` | Physical phone (release APK) | on-device `input` script inside one `adb shell` (no host-latency drift) + `screenrecord` |
| `capture-ios.sh` | iOS Simulator, either app (`compose` / `glass`) | `idb ui tap/swipe --delta 3` + `simctl io recordVideo` |
| `capture-desktop.sh` + `deskdrive.py` | Desktop JVM app window | Quartz synthetic mouse (human-like cursor arcs, trackpad-style continuous scroll) + `screencapture -v -l<window>` |

## Hard-won details baked into these scripts

- **Always re-encode with `-fps_mode cfr -r 60`** — plain ffmpeg transcodes of
  VFR screen recordings silently drop ~1/3 of frames and the motion looks
  rough at any capture rate.
- **iOS**: release K/N builds only (debug is visibly slow); `--delta 3` on
  idb swipes (default touch-point spacing looks robotic); warm-launch the app
  once before the take; status bar via `simctl status_bar override`. If
  `recordVideo` reports "Host recording is already in progress", the recorder
  is orphaned inside SimRender — `simctl shutdown` + `boot` (never kill
  SimRender, it IS the simulator's render server).
- **Android**: DND on + heads-up off or a stray notification will crash the
  take; `cmd statusbar collapse`; run the whole input choreography as ONE
  on-device shell script so `adb` spawn latency can't skew timing; the flight
  menu on the detail page opens from the `…` in the bottom pill.
- **Desktop**: `CGEventPostToPid` delivers hovers but Compose/AWT ignores its
  clicks — post to the HID tap (window must be on the active Space, hands off
  the machine). Wheel: line-unit events work but look chunky; smooth
  "mobile-like" scrolling needs pixel-unit events with
  `kCGScrollWheelEventIsContinuous=1` (trackpad emulation) and a fling-decay
  ease. The window's screenshot shadow padding is asymmetric (56pt sides,
  38pt top) — `deskdrive.py` accounts for it and locates small targets (close
  X, sheet handle, avatar) from live pixels instead of fixed coordinates.
