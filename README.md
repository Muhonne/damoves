# Damoves

Damoves is a native Android app for quickly checking dance-move videos between songs. Search for a move, tap its tile, and watch a short clip before returning to the dance floor.

The app uses Kotlin, Android platform views, and `VideoView`. Clips are bundled for offline playback.

## Use

- Search the grid by dance-move name and tap a tile to play its video once.
- Tap the video to close it and return to the grid. Playback also closes on completion or leaving the app.
- Tap the floating dice button to play a random move from the full library, regardless of the search filter.
- The sound toggle persists across launches.

## Add dance moves

1. Copy each dance-move video into **`app/src/main/res/raw/`**. Use MP4 with H.264 video and AAC audio for broad device compatibility. Use lowercase letters, digits, and underscores in filenames, for example `cross_body_lead.mp4`.
2. Rebuild and reinstall the app. Bundled videos are discovered automatically and listed alphabetically. No code changes are needed.

Filenames become move names: `cross_body_lead.mp4` appears as **Cross body lead**. Underscores become spaces and the first letter is capitalized. Discovery includes `.mp4`, `.m4v`, `.webm`, `.3gp`, and `.mkv` files; other raw resources are ignored. Actual playback support depends on the device and video codec.

Cards automatically extract a frame near the beginning of each video in the background. Separate thumbnail images are unnecessary. The grid uses three columns of rounded cards on a dark background. Each preview is 9:16 for portrait video and center-cropped to fill the card width without padding or letterboxing; move names appear below the preview.

The bundled Kick, Snare, Horn, and Bell entries are sample clips. Replace them with dance-move videos for the intended use.

## Build

Requires JDK 17 or newer, Android SDK platform 37, and the SDK build tools selected by AGP. Configure the SDK through Android Studio or an ignored `local.properties` file containing `sdk.dir=/path/to/android/sdk`. Set `JAVA_HOME` to your JDK for command-line builds.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
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

## Tests

`app/src/test/` contains JVM unit tests for move search and filename discovery. They cover partial and case-insensitive matching, whitespace, blank queries, no results, display names, supported extensions, and ignoring non-video files. JUnit is a test-only dependency; these tests do not require an emulator.

## Device checks

Verify each clip starts, preserves its aspect ratio, and closes on completion. Check tap-to-close, Back, Home, rotation, mute persistence, and search restoration after rotation. A failed clip should show an error and close the player.

## Future development

- Support optional `YYYY_MM_DD_` filename prefixes for newest-first sorting, for example `2026_10_25_cross_body_lead.mp4`. Hide the date in the displayed move name and list undated clips alphabetically after dated clips. Sorting currently remains alphabetical.
