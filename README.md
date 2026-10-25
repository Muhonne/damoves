# Damoves

A native Android video soundboard written in Kotlin using Android platform views and `VideoView`.

- Search the grid and tap a tile to play its bundled video once.
- Playback closes on completion, Stop, or leaving the app.
- The sound toggle persists across launches.
- Videos live in `app/src/main/res/raw/`; thumbnails live in `app/src/main/res/drawable-nodpi/`.
- Add clips to the list in `MainActivity.kt`.

## Build

Requires JDK 17 or newer, Android SDK platform 37, and the SDK build tools selected by AGP. Configure the SDK through Android Studio or an ignored `local.properties` file containing `sdk.dir=/path/to/android/sdk`. Set `JAVA_HOME` to your JDK for command-line builds.

```sh
./gradlew :app:assembleDebug :app:lintDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open the repository in Android Studio to build and run on a device or emulator.

## Setup

- Android Gradle Plugin 9.4.0 with built-in Kotlin support
- Gradle 9.6.0 wrapper
- Compile/target SDK 37; minimum SDK 26 (Android 8.0)
- JVM bytecode target 17
- Kotlin standard library; no third-party UI or playback libraries

## Device checks

Verify each clip starts, preserves its aspect ratio, and closes on completion. Check Stop, Back, Home, rotation, mute persistence, and search restoration after rotation. A failed clip should show an error and close the player.
