# Changelog

## v2.0.1 — installs on older head units

Fixes "There was a problem parsing the package" on units running Android 7.0–8.1.

- **minSdk 28 → 24.** The APK declared Android 9 as its floor; many budget 9"/10"
  heads run 7.1/8.1 under a box that just says "Android System", and their
  installers report a higher-minSdk APK as a parse error rather than an
  incompatibility.
- **Core-library desugaring enabled** so the java.time clock runs below API 26.
- **Release signing now emits v1 (JAR) + v2 signatures explicitly** for maximum
  installer compatibility.

No feature changes. versionCode 7 · versionName "2.0.1"

---

## v2.0 — the driver's release

Version 2 consolidates everything since the first build into one stable line.
No new code over v1.4 — this is the release cut, verified end to end.

- **Car simulation** centre-stage: moves with real OBD-II speed; open doors/hood/boot
  swing out warning-pink with a DOOR OPEN strip (doors simulated/manual — honest seam
  for AAOS/vendor CAN in `CarBodyRepository.tryVehicleSensors()`).
- **Segmented fuel bar**: green ≥60 → yellow 35–59 → orange 15–34 → red <15, pulsing on reserve.
- **Check-engine reader** (OBD-II Mode 03) with plain-English decode; Mode 04 clear
  deliberately not surfaced.
- **Trip computer**: range-to-empty, trip cost in KES, idle-waste cost, eco score.
- **Battery/alternator watch** (0142), throttle (0111), engine load (0104), overspeed alert.
- **Accessories rail**: Bluetooth, Files, Player, Radio, Phone, Camera, Gallery, Browser,
  Equalizer, Sound, Display, Wi-Fi, Storage, All Settings — availability-probed, dimmed when absent.
- **Now Playing panel**: permission-free ⏮ ⏯ ⏭ transport; track metadata after a one-time
  notification-access grant.
- **Hardened**: CarLink receiver non-exported + peer sanitized, ELM327 2 s read-timeout,
  HTTPS-only, narrow `<queries>` instead of QUERY_ALL_PACKAGES.

versionCode 6 · versionName "2.0"

---

## v1.4 — accessories rail + background media panel

**Accessories.** The dock header is now two tabs: **COMMUTER APPS | ACCESSORIES**.
The accessories rail carries Bluetooth, Files, Player, Radio, Phone, Camera, Gallery,
Browser, Equalizer, Sound, Display, Wi-Fi, Storage and All Settings. Each tile is a
list of candidate intents probed against PackageManager — head units are wildly
inconsistent (many have no Files app, vendor FM radios hide behind unguessable package
names), so a tile that can't resolve renders **dimmed and inert** rather than throwing
ActivityNotFoundException at you mid-drive. Costs zero vertical space: the tabs replaced
the old static "COMMUTER APPS" label.

**Now Playing panel.** A compact bar appears above the dock whenever audio is playing —
album art, track, artist, and 52dp prev / play-pause / next buttons (sized to be hit by a
driver without looking). Transport glyphs are drawn on Canvas; pulling in
material-icons-extended for three shapes would have cost megabytes in an APK whose whole
point is 2.8 MB.

The permission story, stated plainly:
- **Controls need no permission at all.** `AudioManager.dispatchMediaKeyEvent` posts the
  same key event a steering-wheel button does; the OS routes it to whichever app owns
  media focus — including your phone's music over Bluetooth A2DP/AVRCP. Works on install.
- **Track title/artist/art needs notification-listener access**, because Android gates
  `MediaSessionManager.getActiveSessions()` behind it. The only alternative,
  MEDIA_CONTENT_CONTROL, is signature|privileged — reserved for apps baked into the system
  image. So you grant it by hand, once: tap "Tap for track info" on the bar.
- Without that grant the panel still appears (via `AudioManager.isMusicActive`) and the
  buttons still work. You just don't see the title. Graceful, not broken.

`TwendeNotificationListener` is an **empty** service — it reads, stores and forwards
nothing. It exists solely to hold the grant. It's `exported="true"` because the system
server must bind it, but it's guarded by BIND_NOTIFICATION_LISTENER_SERVICE, a
signature-level permission only the OS holds — the opposite case from the CarLink
receiver, which had no guard and so was locked down instead.

**`<queries>` block added.** Android 11+ hides other packages by default; a launcher can't
build its drawer or resolve accessory intents without declaring what it needs to see. This
is the narrow, declarative alternative to QUERY_ALL_PACKAGES.

versionCode 5 · versionName "1.4".

---

## v1.3 — fuel bar, vitals, and a check-engine reader

**Segmented fuel bar.** Fuel is now a 14-segment gauge that changes colour by band:
green (>=60%), yellow (35-59%), orange (15-34%), red (<15%, and the lit segments
pulse so it catches your eye without you having to read a number).

**New real PIDs** — all standard, all readable by your generic ELM327:
- `0142` battery / control-module voltage, with a real warning: below 12.4 V at rest
  means a weak battery; below 13.2 V while running means the alternator isn't charging.
  That's a breakdown you get told about *before* it strands you.
- `0111` throttle position, `0104` calculated engine load.

**Check-engine reader (OBD-II Mode 03).** Tap the CHECK ENGINE tile and Twende asks
the car why its light is on, decodes the raw bytes into P/C/B/U codes, and explains the
common ones in plain English ("P0301 — Cylinder 1 misfire"). This is the standout: a
KES-1,500 dongle now does what a garage charges to plug in for. `Mode 04` (clear codes)
is implemented but deliberately NOT wired to a button — clearing a code doesn't fix the
fault and wipes the emissions readiness monitors, which can fail an inspection.

**Trip computer** (`TripComputer`) — derived from real telemetry, not read from the car:
range-to-empty, litres burned, trip cost in KES, idle-waste cost ("you burned KES 40
sitting still"), harsh-acceleration/braking count and an eco score.

**Overspeed alert.** Set a limit (80 km/h = PSV governor); the speed readout turns red
past it.

**Settings:** tank size (litres), fuel price (KES/litre), speed limit.

Honest limits: the fuel *percentage* comes from the car's sender, which is coarse on
many vehicles, so litres/cost/range are good running estimates rather than a fiscal
record. Range is Twende's own estimate, not the car's DTE computer. Harsh-event
thresholds are heuristics on 2 Hz speed samples, not an IMU.

versionCode 4 · versionName "1.3".

---

## v1.2 — center car simulation + live body/telemetry

Replaced the middle compass with an animated top-down **car simulation** that
reacts to the vehicle:

- **Moves with real speed.** The road under the car scrolls in proportion to the
  live OBD-II speed (PID 010D), so the car visibly moves when the vehicle does
  and stops when it stops. RPM/fuel/coolant continue to come from the real bus.
- **Door / hood / boot state.** Any open panel turns warning-pink, swings out,
  and raises a "DOOR OPEN" strip. Heading ("N 0°") moved into this widget.
- **Honest sensor sourcing.** Door-ajar is **not** an OBD-II PID — it's
  manufacturer-specific body-CAN — so on this aftermarket hardware door state is
  driven by a manual tap (tap a quadrant of the car) or a bench simulator.
  `CarBodyRepository.tryVehicleSensors()` is the real seam: on Android Automotive
  OS (CarPropertyManager `DOOR_POS`) or a vendor CAN decoder it lights up with
  live doors automatically. It returns null on generic units rather than faking
  sensor data.
- **No background cost.** The simulator is gated by the same ProcessLifecycleOwner
  foreground signal as OBD — it parks (zero CPU) the moment the launcher backgrounds.

New: `model.BodyStatus`/`Door`, `data/vehicle/CarBodyRepository`,
`ui/home/CarSimulationWidget`. versionCode 3 · versionName "1.2".

---

## v1.1 — security hardening (post pen-test)

Applied after the internal security review of v1. All four are low-effort, and
none change the app's behaviour on your own head unit — they close attack
surface and make the ELM path more robust.

- **CarLink receiver locked down.** `CarLinkBridge` is now `exported="false"`, so
  no other app on the unit (or adb from another UID) can spoof mirroring state.
  Vendor SDKs integrate in-process via `attachVendorSdk()`, which is the correct
  channel anyway. To bench-test the tile you can temporarily flip it back to
  `exported="true"`.
- **Untrusted `peer` sanitized.** The device name from a CarLink broadcast is now
  length-capped (64 chars) and stripped of control characters before it can reach
  the UI or logs.
- **ELM327 read timeout.** `Elm327Client.command()` now reads against a 2000 ms
  deadline and gates on `available()`, so a dead or hostile OBD dongle that never
  sends the `>` prompt can no longer hang the reader thread. Fails safe (that
  cycle's telemetry reads as 0 and polling continues).
- **HTTPS-only enforced.** `android:usesCleartextTraffic="false"` makes the
  platform reject any plaintext request. (The only network call is HTTPS to
  Google Places; this just makes it structural.)

### Still recommended (outside the APK)
- Restrict your Google Places API key in the Cloud console to the app's package
  name + signing-cert SHA-1. That's the real fix for key theft — far stronger
  than any at-rest encryption in the app.
- For Play Store distribution, replace the CI's throwaway keystore with one held
  in GitHub Secrets so every build shares a stable signing identity.

versionCode 2 · versionName "1.1"
