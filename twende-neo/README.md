# Twende Launcher v2 — Kotlin/Compose edition

A futuristic, ad-free home-screen replacement for Android head units.
Single Activity, 100% Kotlin + Jetpack Compose, zero XML on the render path,
targeting API 34 with minSdk 24 (Android 7.0+ — covers the 7.1/8.1/9/10 spread
actually found on budget head units, whatever the box claims).

## Build & install (recommended: CI, no local tooling)

1. Upload this folder's contents into `twende-neo/` of
   `github.com/denisfmbae/twende-launcher` (browser: **Add file → Upload files**).
2. The commit auto-triggers **Actions**; ~5 min later the signed
   `app-release.apk` appears under **Releases**.
3. Sideload to the unit, press **Home**, choose **Twende Launcher → Always**.
   Play Protect will warn about an unrecognised developer — **More details →
   Install anyway** (or use the install page, which walks through it).

Local alternative: Android Studio (JDK 17) → open folder → `./gradlew :app:assembleRelease`.

## What's wired vs. stubbed

| Area | Status |
|---|---|
| App launching, drawer, commuter dock | ✅ live (PackageManager) |
| Clock, date, status dots | ✅ live |
| Car simulation (centre) | ✅ moves with real OBD speed; doors/hood/boot warn pink when open (simulated/manual — doors aren't an OBD-II PID; AAOS seam documented) |
| Heading | ✅ live — rotation-vector sensor, shown inside the car widget |
| Fuel bar + range/cost/eco/idle | ✅ bar & vitals derived from live telemetry; tank size, KES/L, speed limit in Setup |
| Check-engine codes (Mode 03) | ✅ live read + plain-English decode; simulated demo codes when the bench simulator is on |
| Accessories rail | ✅ availability-probed intents; unresolvable tiles dim instead of crashing |
| Now Playing (background media) | ✅ transport works with zero permission (media-key events); title/art after one-time notification-access grant |
| GPS speed / location | ✅ live — FusedLocationProvider, 1 Hz |
| OBD-II telemetry (RPM, speed, fuel, temp @500 ms) | ✅ live via ELM327 over BT SPP; **simulator on by default** (Setup tab) |
| Bluetooth HFP + A2DP status, device battery | ✅ live observation; battery uses the de-facto `BATTERY_LEVEL_CHANGED` broadcast (OEM-dependent) |
| Predictive POI search (fuel- and time-aware) | ✅ query builder live; network needs a Places API key in Setup, otherwise offline suggestions |
| CarLink 2.0 | 🔌 documented stub — proprietary; test with:<br>`adb shell am broadcast -a co.nedlink.twende.carlink.SESSION_STATE --ez connected true --es peer "Ned's Phone"` |
| Recorder dot | 🔌 stub — wire to the unit's DVR broadcast |

## Performance playbook

- **Cold start:** launch theme paints `#0B0C10` before Compose attaches; DI graph
  is two providers; no ContentProvider initializers. On a 2 GB unit expect the
  first meaningful frame well inside the 500 ms budget after a warm profile.
  For guaranteed numbers add a Baseline Profile module (macrobenchmark) — the
  release build is already R8-minified and resource-shrunk.
- **Frame rate:** every animation is duration-based (`tween`, infinite
  transitions), so it renders correctly at whatever the panel refreshes —
  note that most head-unit LCDs are 60 Hz; "120 fps" is a ceiling, not a floor.
- **Recomposition:** streams are `stateIn(WhileSubscribed(2000))` — leave the
  home screen and every sensor/BT/OBD collector stops within 2 s. The
  `ProcessLifecycleOwner` gate in `TwendeApp` additionally parks the OBD
  polling loop when any launched app takes the foreground. RAM stays far
  under the 150 MB envelope: one Activity, no fragments, icons pre-rasterised
  to 96 px.
- **Glassmorphism:** true backdrop blur needs API 31 `RenderEffect`; on 28–30
  units it costs frames, so cards use a 10 % white fill + gradient hairline
  that reads identically at arm's length.

## No ads, structurally

No ad SDKs, no analytics, no foreground services. The single network call in
the entire codebase is the Places search you explicitly key in Setup. Remove
the `INTERNET` permission from the manifest and the app degrades gracefully
to offline POI suggestions.
