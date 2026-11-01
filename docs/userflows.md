# User flows

How Damoves behaves from the dancer's side, screen by screen. The [README](../README.md) covers setup and building; this file covers what each interaction does and what happens at the edges.

## Browse and search moves

1. The app opens on a three-column grid of rounded cards, each showing a frame from the clip with the move name wrapped over the bottom.
2. Type in the search box to filter by name. Matching is partial and case-insensitive, and the query survives rotation.
3. The header, sound toggle, and search box stay fixed; the grid scrolls beneath them, with enough padding that the last row clears the floating dice.

## Play a move from a tile

1. Tap a tile. The clip opens full screen, keeping its aspect ratio.
2. Tap the video to close it early.
3. If you let it run, a tile selection closes itself when the clip finishes.
4. Pressing Back or Home stops playback and returns to the grid.

## Play a random move with the dice

1. Tap the floating dice button to loop a random move from the **full** library. The dice ignores the current search filter.
2. The same move repeats until you close it. Tap the video, or press Back or Home.

## Add a move from a gallery clip

Reached from **Add clip**. The system gallery/document picker supplies the video, so the app needs no broad gallery permission.

1. **Pick a clip.** The picker opens the video file directly in the importer. If a clip cannot be played, an error is shown and the importer closes.
2. **Choose a range.** Drag either handle on the two-handle timeline; the label above reads `Start x.xx s — End y.yy s`.
3. **Preview the selection.** **Preview selection** plays from the range start and stops at the range end, returning to the start. Tap it again, or let it reach the end, to go back to **Preview selection**.
4. **Scrub the video.** **−5s** and **+5s** move the playhead five seconds, clamped to the clip.
5. **Fine-tune.** **Start −**, **Start +**, **End −**, and **End +** nudge by 0.5 seconds. Press and hold to keep adjusting until you release.
6. **Name and save.** Type a move name, then tap **Save move**. While the trim is written, the button disables and reads `Saving…`.

The selection cannot be inverted: the start stays at least one step below the end, and both stay inside the clip.

### What saving does

- The selected range is stream-copied into a private MP4 in app storage, so there is no re-encoding and no quality loss.
- The saved start snaps back to the preceding video keyframe, which can include a little extra footage before your selection.
- Export depends on the source codecs; MP4 with H.264 video and AAC audio is the safe choice.
- The original video is never modified or moved.
- A move that fails to save reports `Could not save this video. Try an MP4 video.` and leaves you in the importer.

### After saving

The importer closes and the new move joins the grid, search, and dice selections, available offline like bundled moves. It is stored privately in the app: uninstalling Damoves removes imported moves.

## Manage imported clips

1. Press and hold an imported tile to open its options. Bundled moves shipped in the app do not respond, because they cannot be changed or removed.
2. **Edit clip** re-opens that clip in the importer with its current name filled in, so you can adjust the range and save again.
3. **Delete** asks for confirmation, then removes the clip and its name from the app.

Editing works on the app's own trimmed copy, not the original gallery video: the app keeps no link back to the source file. Saving from the editor always writes a new move, so a re-trim leaves the earlier one in place until you delete it. Deleting an imported clip never touches the original video, which stays where it is.

## Sound

- The header toggle mutes or unmutes playback and is labelled `Sound on` / `Sound off`.
- The choice persists across launches and applies to the next clip played.
- Playback still respects the choice when the app is restarted.

## Leaving the importer

- Back or Home stops preview playback and discards the import.
- Rotating the device keeps the chosen range and the typed name.

## Edge cases

- A clip that cannot be opened, prepared, or played shows a playback error and closes the player.
- Trimmed imports live in app-private storage; clearing the app's data or uninstalling removes them.
- Bundled moves stay in the APK and are unaffected by imports.
