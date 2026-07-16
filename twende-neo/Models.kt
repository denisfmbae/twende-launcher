name: Build Twende APK

# Runs on every push, and on demand from the Actions tab (Run workflow).
on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  apk:
    runs-on: ubuntu-latest   # ships with the Android SDK preinstalled
    permissions:
      contents: write        # needed to publish the APK as a Release

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Gradle 8.7
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.7'   # matches AGP 8.5.2; no wrapper jar needed in the repo

      - name: Generate a sideload keystore
        # Throwaway key, valid 27 years. Good for sideloading onto your head unit.
        # For Play Store you'd swap this for a keystore held in GitHub Secrets.
        run: |
          keytool -genkeypair -v \
            -keystore "$RUNNER_TEMP/twende.jks" \
            -alias twende \
            -keyalg RSA -keysize 2048 -validity 10000 \
            -storepass twende123 -keypass twende123 \
            -dname "CN=Nedlink Solutions, OU=Twende, O=Nedlink, L=Meru, C=KE"

      - name: Build release APK
        continue-on-error: true   # if R8 trips, we still ship the debug APK below
        env:
          TWENDE_KEYSTORE: ${{ runner.temp }}/twende.jks
          TWENDE_STORE_PASS: twende123
          TWENDE_KEY_ALIAS: twende
          TWENDE_KEY_PASS: twende123
        run: gradle assembleRelease --no-daemon --stacktrace

      - name: Build debug APK (always installable fallback)
        run: gradle assembleDebug --no-daemon --stacktrace

      - name: Collect APKs
        run: |
          mkdir -p out
          find app/build/outputs/apk -name "*.apk" -exec cp {} out/ \;
          ls -lh out

      - name: Upload APKs (Actions artifact)
        uses: actions/upload-artifact@v4
        with:
          name: twende-apk
          path: out/*.apk
          if-no-files-found: error

      - name: Publish APK as a Release
        # Gives you a permanent, plain download link you can open on your phone
        # or straight from the head unit's browser — no GitHub login needed.
        uses: softprops/action-gh-release@v2
        with:
          tag_name: build-${{ github.run_number }}
          name: Twende Launcher build ${{ github.run_number }}
          files: out/*.apk
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
