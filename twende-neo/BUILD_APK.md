# Getting the Twende APK

An APK has to be compiled with the Android SDK. Pick whichever route fits what you have in front of you.

---

## Route A — GitHub Actions (no Android Studio, no SDK, works from a phone)

The workflow at `.github/workflows/build-apk.yml` builds the APK on GitHub's runners, which already have the Android SDK.

1. Create a new repo on GitHub (private is fine), e.g. `twende-launcher`.
2. Upload this project folder to it — either:
   - **Web:** repo → *Add file* → *Upload files* → drag the unzipped folder in, or
   - **Git:**
     ```bash
     git init && git add . && git commit -m "Twende Launcher v1"
     git branch -M main
     git remote add origin https://github.com/<you>/twende-launcher.git
     git push -u origin main
     ```
3. Go to the **Actions** tab. The build starts on push, or hit **Run workflow**.
4. When it goes green (~4–6 min), open the run and download the **`twende-apk`** artifact.
5. Inside: `app-release.apk` (fast, R8-shrunk — use this one) and `app-debug.apk` (fallback).

The workflow generates a throwaway signing key on the runner, so the release APK is signed and installable. No keystore ever lives in the repo.

> Play Store later: replace the `Generate a sideload keystore` step with a real keystore pulled from GitHub Secrets, so updates keep the same signature.

---

## Route B — Android Studio (best if you want to iterate)

1. Android Studio **Koala or newer**, JDK 17.
2. *File → Open* → this folder. Let it sync (it generates the Gradle wrapper for you).
3. *Build → Build Bundle(s)/APK(s) → Build APK(s)*.
4. APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

Or plug the head unit in over USB and just hit **Run** — it installs directly.

---

## Route C — Command line, no Studio

Needs JDK 17 and Android `cmdline-tools`.

```bash
sdkmanager "platforms;android-34" "build-tools;34.0.0"
export ANDROID_HOME=$HOME/Android/Sdk
gradle wrapper --gradle-version 8.7   # one time
./gradlew assembleDebug
```

---

## Installing on the head unit

1. Copy the APK to a USB stick or SD card, plug it into the unit.
2. Open the unit's file manager → tap the APK → allow *Install unknown apps* if prompted.
   - Or over adb, if the unit exposes it: `adb install -r app-release.apk`
3. Press **Home**. Android asks which launcher to use → pick **Twende** → **Always**.

### Escape hatch (read this before you set it as Home)

If Twende misbehaves, you can still get back:
*Settings → Apps → Twende Launcher → Clear defaults* (or *Open by default → Clear*). The stock launcher returns on the next Home press. Keep the stock launcher installed — don't uninstall it.

### First run

- Grant **Location** (compass + speed) and **Nearby devices / Bluetooth** when asked.
- The **OBD simulator is ON by default** — gauges animate on the bench, no car needed. Turn it off in *Settings* once your ELM327 is paired.
- Places API key is optional; leave it blank and the POI bar serves offline suggestions.

### Test the CarLink stub without a phone

```bash
adb shell am broadcast -a co.nedlink.twende.carlink.SESSION_STATE \
  --ez connected true --es peer "Ned's Phone"
```

---

## If a build fails

Grab the red step's log from Actions and send it over — the usual suspects are an SDK licence prompt (handled on the runner) or an R8 rule. The debug APK is built unconditionally, so you'll have something installable either way.
