# Damoves

## Purpose

Help dancers check dance-move videos quickly between songs. Prioritize finding a move, watching a short clip, and returning to the grid with few taps. Keep bundled clips available offline.

## Development

- Keep both apps small and native: Kotlin, Android platform views, and `VideoView` on Android; SwiftUI and AVFoundation on iOS.
- Prefer simple, robust changes. Add dependencies only when they solve a clear need.
- Keep search, readable move names, thumbnails, and playback responsive.
- Stop and release playback when leaving the player. Preserve video aspect ratio and the sound preference.
- Moves opened from a tile play once; dice selections loop the same move until closed. Keep the dice independent of the search filter.
- Keep cards at 9:12 (width:height), with wrapping labels over the bottom of the thumbnail. Keep the header outside the scrolling grid and let the dice float over the grid.
- Copy videos to `android/app/src/main/res/raw/` and/or `ios/Damoves/Videos/`; discover moves automatically from bundled video filenames, with no manual registry. Card thumbnails come directly from video frames.
- Derive move labels from resource entry names, never packaged file basenames: release packaging shortens resource paths. Verify readable move labels in the signed release.
- Keep the UI minimal and dark. Refer to dance clips as moves; sound refers only to audio settings.
- Update the [README](README.md) for setup changes and [User flows](docs/userflows.md) for behavior changes.

## Documentation

- [README](README.md): what Damoves is, use, adding moves, build, signed release, tests, device checks.
- [User flows](docs/userflows.md): how each screen behaves, screen by screen, and what happens at the edges.
- [Android tooling](docs/android-tooling.md): Android Studio, JDK 25, SDK, emulator, and signed device installation workflows.

## Validation

For Android changes, run `cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`. For iOS changes, run `cd ios && swift test` and build the Xcode project for an iOS simulator. For playback or UI changes, check affected behavior on the relevant device or emulator, including completion, tap-to-close, Back (Android), Home, search, rotation, and mute persistence.
For Android release/signing changes, also run `./gradlew :app:assembleRelease :app:lintRelease` from `android/` and verify the APK signature. Keep release signing material outside the repository.

## Git

Never merge a pull request or enable auto-merge without explicit user approval for that PR. Do not create worktrees unless requested or separate work requires one.
Never commit local videos, APKs, signing keys, passwords, SDK paths, or IDE/build caches. Verify videos are ignored and absent from the staged file list before committing.
