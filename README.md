# Damoves

Damoves is a native Android and iOS app for quickly checking dance-move videos between songs. Search for a move, tap its tile, and watch a short clip before returning to the dance floor. Bundled and imported clips are available offline.

Android uses Kotlin, platform views, and `VideoView`; iOS uses SwiftUI and AVFoundation. The Android launcher icon uses Google Noto Emoji; see `licenses/noto-emoji-LICENSE.txt`.

## Use

- Search the grid by move name and tap a tile to play its video once. Tap the video to close it; leaving the app stops playback.
- The floating dice loops a random move from the full library, regardless of the search filter. The sound toggle persists across launches.
- **Add clip** opens the system picker (Photos or Files on iOS). Choose a range, preview it, fine-tune the start/end in 0.5-second steps, enter a name, and save. The private copy appears in the grid, search, and dice; the original is untouched.
- Long-press an imported move to edit it as a new copy or delete it. Uninstalling the app removes imported moves.
- Trimming uses fast passthrough export. The saved range can start at an earlier video keyframe. Export support depends on source codecs; MP4 with H.264/AAC is recommended.

See [User flows](docs/userflows.md) for screen-by-screen behavior and platform differences.

## Add bundled moves

Copy clips to the platform(s) you build, then rebuild and reinstall. No registry or separate thumbnails are needed. Files are ignored by Git; a fresh checkout has no bundled clips.

- Android: `android/app/src/main/res/raw/`. Use lowercase letters, digits and underscores, e.g. `cross_body_lead.mp4`. Discovery supports MP4, M4V, WebM, 3GP and MKV; device codec support varies. Labels use Android resource entry names, not packaged file paths, so release packaging cannot shorten them.
- iOS: `ios/Damoves/Videos/`. The folder is copied into the app bundle, including newly added files automatically. Discovery supports MP4, M4V, MOV and 3GP; actual playback depends on AVFoundation and the codecs. H.264/AAC MP4 is a good choice for both platforms.

Underscores become spaces and the first character is capitalized: `cross_body_lead.mp4` becomes **Cross body lead**. Cards extract a frame near the beginning and center-crop it to a 9:12 tile with a wrapping label.

## Android build

Requires JDK 25 for Gradle, Android SDK platform 37 and AGP's selected build tools. Configure the SDK through Android Studio or ignored `android/local.properties`. Open `android/` in Android Studio. See [Android tooling](docs/android-tooling.md) for SDK, emulator and device workflows.

```sh
cd android
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Gradle 9.6.0, AGP 9.4.1, Kotlin 2.4.21, min SDK 26 and Java 17 bytecode are configured in the project.

### Signed Android release

Release signing reads `~/.local/share/damoves/signing/release.properties`, or `-PdamovesSigningProperties=/absolute/path/release.properties`. This private file contains `storeFile` (absolute keystore path), `storePassword`, `keyAlias`, and `keyPassword`. Keep it and the keystore outside the repository and back them up securely.

```sh
cd android
./gradlew :app:assembleRelease :app:lintRelease
adb -s DEVICE_SERIAL install -r app/build/outputs/apk/release/app-release.apk
```

Without signing properties the APK is unsigned. A differently signed release cannot replace the debug app; uninstalling resets local data. Future updates require the same key and an increased `versionCode`. Verify the signature and readable move labels on the signed release; see [Android tooling](docs/android-tooling.md).

## iOS build

Requires Xcode with the iOS SDK and iOS 16 or later. Open `ios/Damoves.xcodeproj`, select the Damoves scheme and an iOS simulator, and run. To install on a physical device, select your Apple developer team and a unique bundle identifier under Signing & Capabilities; Xcode manages signing. Do not commit personal signing settings.

From the command line with Xcode selected:

```sh
cd ios
swift test
xcodebuild -project Damoves.xcodeproj -scheme Damoves -sdk iphonesimulator CODE_SIGNING_ALLOWED=NO build
```

## Tests and device checks

Android JVM tests cover search and discovery; iOS Swift package tests cover names, search, and range clamping. They do not replace device checks. On each platform verify aspect ratio, tile completion, dice looping, tap-to-close, Home, search, rotation, sound persistence, import preview/save/edit/delete, and error handling. On Android also check Back. Verify long labels, the fixed header and the last row clearing the dice.
