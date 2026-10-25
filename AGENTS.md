# Damoves

## Purpose

Help dancers check dance-move videos quickly between songs. Prioritize finding a move, watching a short clip, and returning to the grid with few taps. Keep bundled clips available offline.

## Development

- Keep the app small and native: Kotlin, Android platform views, and `VideoView`.
- Prefer simple, robust changes. Add dependencies only when they solve a clear need.
- Keep search, readable move names, thumbnails, and playback responsive.
- Stop and release playback when leaving the player. Preserve video aspect ratio and the sound preference.
- Store videos in `app/src/main/res/raw/`, thumbnails in `app/src/main/res/drawable-nodpi/`, and move entries in `MainActivity.kt`.
- Update the README when setup or behavior changes.

## Validation

Run `./gradlew :app:assembleDebug :app:lintDebug` for code or build changes. For playback or UI changes, check affected behavior on a device or emulator, including completion, Stop, Back, Home, search, rotation, and mute persistence.

## Git

Never merge a pull request or enable auto-merge without explicit user approval for that PR. Do not create worktrees unless requested or separate work requires one.
