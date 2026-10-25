# Android tooling

## Installed workstation setup

- Android Studio Rabbit 1, 2026.2.1 (build `262.9437.185.2621.16467767`). Installed from `~/Downloads/android-studio-rabbit1-linux.tar.gz` into `~/.local/opt/android-studio`.
- Start from the Android Studio application menu entry, or run `~/.local/opt/android-studio/bin/studio /path/to/damoves`.
- Bundled Java: `~/.local/opt/android-studio/jbr` (JBR 25). Gradle daemon criteria pin Java 25; the app's bytecode target remains Java 17.
- SDK: `~/Android/Sdk`. Platform 37 and build tools 36.0.0 are installed; AGP selects its required build tools.
- Emulator: `Medium_Phone_API_37.0`, with KVM acceleration available on this workstation.
- Physical device: Pixel 9a. Damoves is installed as a signed, non-debuggable release with all 28 local videos bundled for offline use. It runs without Android Studio or a computer connection.

These paths describe this workstation; keep machine-specific SDK configuration in the ignored `local.properties`. On another machine, install Android Studio and the required SDK packages through its setup wizard and SDK Manager. Do not commit the SDK or IDE installation.

## Open, sync, and build

Open the repository root in Android Studio. Let Gradle sync finish before running `app`. If Run is disabled, inspect the Sync/Build output, confirm SDK platform 37 is installed, and use **File > Sync Project with Gradle Files**. Ensure the Gradle JVM is Java 25; use the bundled JBR rather than an unrelated system Java installation.

For command-line builds on this workstation:

```sh
export JAVA_HOME="$HOME/.local/opt/android-studio/jbr"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The project uses AGP 9.4.1, Gradle 9.6.0, and Kotlin 2.4.21 through AGP's built-in Kotlin support. The root build file supplies the Kotlin Gradle plugin dependency; do not apply the separate `org.jetbrains.kotlin.android` plugin alongside built-in Kotlin.

## Local videos

Copy personal clips to `app/src/main/res/raw/` before building, using lowercase Android resource filenames containing only letters, digits, and underscores. For example, `inside turn-rock go.mp4` becomes `inside_turn_rock_go.mp4`. Clips are discovered automatically; no registry is needed.

The repository contains no video files in the current tracked tree. The old Bell, Horn, Kick, and Snare samples were removed. `.gitignore` excludes MP4, M4V, WebM, 3GP, and MKV files, including uppercase extensions. Local videos remain on disk and are bundled into locally built APKs. A fresh clone needs its own clips.

Before committing, check:

```sh
git check-ignore app/src/main/res/raw/*.mp4
git ls-files '*.mp4' '*.m4v' '*.webm' '*.3gp' '*.mkv'
git diff --cached --name-only
```

The `git ls-files` command should list no videos. APKs are roughly 1 GB with the current library, so device transfers can take a while.

## Emulator and USB device

Start the emulator through Android Studio's Device Manager. For a phone, enable Developer options and USB debugging, connect it, and accept the computer's debugging authorization on the phone.

```sh
adb devices -l
adb -s DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s DEVICE_SERIAL shell am start -n com.example.soundboard/.MainActivity
```

Select the target explicitly when both the emulator and phone are connected. Use the emulator for debug builds: the Pixel's installed release uses a different signing key, so a debug APK cannot update it. Do not uninstall the Pixel release just to run a debug build unless resetting its local data is intended.

## Release updates for the Pixel

Signing material is stored outside the repository in `~/.local/share/damoves/signing/`:

- `damoves-release.p12`: the reusable release signing key.
- `release.properties`: keystore path, alias, and passwords, readable only by the owner.

Back up this entire folder securely. Future releases must retain the same application ID (`com.example.soundboard`) and signing key. Do not regenerate the key for each build, publish these files, or print their passwords in logs. A different workstation can supply a private properties file with `-PdamovesSigningProperties=/absolute/path/release.properties`.

Increment `versionCode` and update `versionName` in `app/build.gradle` when making a new release, then:

```sh
./gradlew :app:testDebugUnitTest :app:assembleRelease :app:lintRelease
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose --print-certs \
  app/build/outputs/apk/release/app-release.apk
adb -s DEVICE_SERIAL install -r app/build/outputs/apk/release/app-release.apk
adb -s DEVICE_SERIAL shell am start -n com.example.soundboard/.MainActivity
```

The signature verification must succeed before installation. Check readable move labels in the signed release itself: release packaging shortens resource file paths, so labels must use resource entry names rather than APK basenames. Without signing properties, Gradle produces an unsigned release APK instead. Installing future signed releases with `-r` preserves settings. The one-time switch from debug to release required uninstalling the old app and reset its local settings; this migration is already complete on the Pixel.
