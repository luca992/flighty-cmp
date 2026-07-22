# Walkthrough capture scripts

Scripted, reproducible captures of the same ~25s demo choreography on every
platform, plus the side-by-side composite: home → flight detail → scroll to
bottom → flight-settings menu → scroll back → pull the sheet down to reveal
the map → close → Friends → Passport → Add Flight → profile menu.

| Script | Target | Input mechanism |
| --- | --- | --- |
| `capture-android.sh` | Physical phone (release APK) | on-device `input` script inside one `adb shell` (no host-latency drift) + `screenrecord` |
| `capture-ios.sh` | iOS Simulator, either app (`compose` / `glass`) | `idb ui tap/swipe --delta 3` + `simctl io recordVideo` |
| `capture-desktop.sh` + `deskdrive.py` | Desktop JVM app window | Quartz synthetic mouse (human-like cursor arcs, trackpad-style continuous scroll) + `screencapture -v -l<window>` |
| `build-composite.sh` | `demo-captures/composite.mp4` + poster + GIF | ffmpeg: beat-aligned trims, loop/select retimes, PIL-rendered header, rounded masks |

## Hard-won details baked into these scripts

- **Always re-encode with `-fps_mode cfr -r 60`** — plain ffmpeg transcodes of
  VFR screen recordings silently drop ~1/3 of frames and motion looks rough.
- **iOS**: release K/N builds only (debug is visibly slow); `--delta 3` on idb
  swipes (default touch-point spacing looks robotic); warm-launch once before
  the take; `simctl status_bar override --time 9:41`. If `recordVideo` says
  "Host recording is already in progress", an orphaned recording lives inside
  SimRender — `simctl shutdown` + `boot` (never kill SimRender: it IS the
  simulator's render server).
- **Android**: DND on + heads-up off or a stray notification lands in the
  take; `cmd statusbar collapse`; run the whole choreography as ONE on-device
  shell script so adb spawn latency can't skew timing.
- **Desktop**: `CGEventPostToPid` delivers hovers but Compose/AWT ignores its
  clicks — post to the HID tap (window on the active Space, hands off).
  Smooth "mobile-like" scrolling = pixel-unit wheel events with
  `kCGScrollWheelEventIsContinuous=1` and a fling-decay ease (line units work
  but look chunky; pixel units WITHOUT the continuous flag are ignored).
  Window screenshots have asymmetric shadow padding (56pt sides, 38pt top).
  **Restart the app for every take** — blind clicks against an assumed UI
  state are how takes die; the two targets that move with sheet state
  (detail-close X, avatar) are located from live pixels mid-take.
- **Composite sync**: measure each capture's detail-open (first sustained
  motion in a 30fps frame-diff scan) and trim so it lands at t=1.6; then
  align the detail-close with pre-close freezes (`loop=loop=N:size=1:start=F`)
  or a windowed speed-up (`select='not(between(t,A,B)*lt(mod(n,7),2))'` =
  1.4x). Verify beats by extracting composite frames, not by trusting color
  probes — a "close detector" probing for the avatar's purple matched a blue
  progress bar and lied. GitHub READMEs don't render committed mp4s: embed
  the palette-optimized GIF and link the mp4.
