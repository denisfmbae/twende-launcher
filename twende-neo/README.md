# Twende Launcher — Kotlin/Compose edition

A futuristic, ad-free home-screen replacement for Android head units.
Single Activity, 100% Kotlin + Jetpack Compose, zero XML on the render path,
targeting API 34 with minSdk 28 (covers Android 9/10 budget units).

## Build & install

1. Android Studio (Koala or newer, JDK 17) → **Open** this folder.
2. Let Gradle sync (AGP 8.5.2 / Kotlin 2.0.20 / KSP / Hilt via version catalog).
3. `Build → Build APK`, or from CLI: `./gradlew :app:assembleRelease`
   *(no Gradle wrapper is committed — Studio generates one, or run `gradle wrapper`)*
4. Sideload to the unit (`adb install` or USB stick), press **Home**,
   choose **Twende Launcher → Always**.

## What's wired vs. stubbed

| Area | Status |
|---|---|
| App launching, drawer, commuter dock | ✅ live (PackageManager) |
| Clock, date, status dots | ✅ live |
| Compass ("North 0°") | ✅ live — rotation-vector sensor, unwrapped continuous animation |
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
