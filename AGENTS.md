# Damoves

## Purpose

Help dancers check dance-move videos quickly between songs. Prioritize finding a move, watching a short clip, and returning to the grid with few taps. Keep bundled clips available offline.

## Development

- Keep the app small and native: Kotlin, Android platform views, and `VideoView`.
- Prefer simple, robust changes. Add dependencies only when they solve a clear need.
- Keep search, readable move names, thumbnails, and playback responsive.
- Stop and release playback when leaving the player. Preserve video aspect ratio and the sound preference.
- Moves opened from a tile play once; dice selections loop the same move until closed. Keep the dice independent of the search filter.
- Keep cards at 9:12 (width:height), with wrapping labels over the bottom of the thumbnail. Keep the header outside the scrolling grid and let the dice float over the grid.
- Copy videos to `app/src/main/res/raw/`; discover moves automatically from bundled video filenames, with no manual registry. Card thumbnails come directly from video frames.
- Keep the UI minimal and dark. Refer to dance clips as moves; sound refers only to audio settings.
- Update the README when setup or behavior changes.
- See `docs/android-tooling.md` for Android Studio, JDK 25, SDK, emulator, and signed device installation workflows.

## Validation

Run `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` for code or build changes. For playback or UI changes, check affected behavior on a device or emulator, including completion, tap-to-close, Back, Home, search, rotation, and mute persistence.
For release/signing changes, also run `./gradlew :app:assembleRelease :app:lintRelease` and verify the APK signature. Keep release signing material outside the repository.

## Git

Never merge a pull request or enable auto-merge without explicit user approval for that PR. Do not create worktrees unless requested or separate work requires one.
Never commit local videos, APKs, signing keys, passwords, SDK paths, or IDE/build caches. Verify videos are ignored and absent from the staged file list before committing.
