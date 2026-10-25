# Damoves

Damoves is a native Android app for quickly checking dance-move videos between songs. Search for a move, tap its tile, and watch a short clip before returning to the dance floor.

The app uses Kotlin, Android platform views, and `VideoView`. Clips are bundled for offline playback.

The launcher icon pairs the dancing woman and man emoji (💃🕺) from Google Noto Emoji on a muted rainbow gradient with adaptive icon support. Artwork license: `licenses/noto-emoji-LICENSE.txt`.

## Use

- Search the grid by dance-move name and tap a tile to play its video once.
- Tap the video to close it and return to the grid. Moves opened from a tile close on completion; dice selections repeat. Leaving the app closes playback.
- Tap the floating dice button to loop a random move from the full library until closed, regardless of the search filter.
- The sound toggle persists across launches.

## Add dance moves

1. Copy each dance-move video into **`app/src/main/res/raw/`**. Use MP4 with H.264 video and AAC audio for broad device compatibility. Use lowercase letters, digits, and underscores in filenames, for example `cross_body_lead.mp4`.
2. Rebuild and reinstall the app. Bundled videos are discovered automatically and listed alphabetically. No code changes are needed.

Video files are ignored by Git; copy your local clips into this folder before building.

Filenames become move names: `cross_body_lead.mp4` appears as **Cross body lead**. Underscores become spaces and the first letter is capitalized. Discovery includes `.mp4`, `.m4v`, `.webm`, `.3gp`, and `.mkv` files; other raw resources are ignored. Actual playback support depends on the device and video codec.

Cards automatically extract a frame near the beginning of each video in the background. Separate thumbnail images are unnecessary. The grid uses three columns of rounded cards on a dark background. Each preview is 9:12 (width to height) and center-cropped to fill the card width without padding or letterboxing. Move names overlay the bottom of each preview on a translucent dark background and wrap to the height needed for the full name.

The compact title, sound toggle, and search stay above a thin divider. The move grid scrolls within its own area below the header.
The dice floats over the grid, with enough scroll space to bring the last row above it.

## Build

Requires JDK 25 for the Gradle daemon, Android SDK platform 37, and the SDK build tools selected by AGP. Java/Kotlin bytecode targets Java 17. Configure the SDK through Android Studio or an ignored `local.properties` file containing `sdk.dir=/path/to/android/sdk`. See [Android tooling](docs/android-tooling.md) for the installed Android Studio, SDK, emulator, and Pixel workflows.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open the repository in Android Studio to build and run on a device or emulator.

## Signed release for a personal device

Release signing reads `~/.local/share/damoves/signing/release.properties`, or the path supplied with `-PdamovesSigningProperties=/absolute/path/release.properties`. This private file contains `storeFile` (absolute keystore path), `storePassword`, `keyAlias`, and `keyPassword`. Keep it and the keystore outside the repository and back them up securely: future updates must use the same signing key.

```sh
./gradlew :app:assembleRelease :app:lintRelease
adb -s DEVICE_SERIAL install -r app/build/outputs/apk/release/app-release.apk
```

Without signing properties, the release APK is unsigned. A release signed with a different key cannot replace the debug app; uninstall the debug app once before installing the first release (this resets local app settings). Subsequent releases can update it in place. Increase `versionCode` for new releases.

## Setup

- Android Studio Rabbit 1 (2026.2.1), bundled JBR 25
- Android Gradle Plugin 9.4.1 with built-in Kotlin support, using Kotlin 2.4.21
- Gradle 9.6.0 wrapper
- Gradle daemon JVM 25, pinned in `gradle/gradle-daemon-jvm.properties`
- Compile/target SDK 37; minimum SDK 26 (Android 8.0)
- JVM bytecode target 17
- Kotlin standard library; no third-party UI or playback libraries

## Tests

`app/src/test/` contains JVM unit tests for move search and filename discovery. They cover partial and case-insensitive matching, whitespace, blank queries, no results, display names, supported extensions, and ignoring non-video files. JUnit is a test-only dependency; these tests do not require an emulator.

## Device checks

Verify each clip starts and preserves its aspect ratio. Tile selections should close on completion; dice selections should repeat the same move until closed. Check tap-to-close, Back, Home, rotation, mute persistence, and search restoration after rotation. Verify long labels wrap fully, the header stays separate when scrolling, and the last row scrolls above the floating dice. A failed clip should show an error and close the player.

## Future development

- Support optional `YYYY_MM_DD_` filename prefixes for newest-first sorting, for example `2026_10_25_cross_body_lead.mp4`. Hide the date in the displayed move name and list undated clips alphabetically after dated clips. Sorting currently remains alphabetical.
