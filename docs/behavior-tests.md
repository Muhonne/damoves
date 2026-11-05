# Headless behavior tests

[Shopify's Reflex article](https://shopify.engineering/reflex) describes an architecture, not a published library. Damoves implements a subset: UI-independent feature cores per platform, an action/state interface, deterministic fixtures, and one suite that runs against both implementations. There is no Shopify dependency and no introspection server in either app.

## Run

On macOS, install Xcode, Node.js 18 or later, and JDK 25. Set `JAVA_HOME` to a JDK 25 installation. The headless suite does not require the Android SDK; Android app builds still do. From the repository root:

```sh
./tests/behavior/run.sh
```

The script compiles the Swift host into a temporary directory and builds the JVM-only Gradle `behaviorHost` distribution. Gradle may download dependencies on the first run. Tests themselves use only in-process fixtures and do not intentionally access the network; there is currently no network-blocking guard. A simulator or emulator is not needed.

GitHub Actions runs this same command on a macOS runner for pushes and pull requests; it can also be started manually from the Actions tab. The workflow is `.github/workflows/behavior-tests.yml`. It does not build the mobile apps or run their separate unit or device tests.

`BrowseCore`, `EditorCore`, `PlayerCore`, and `NavigationCore` in `ios/Damoves/` and `android/app/src/main/java/com/example/soundboard/` are used by the mobile apps. `ios/BehaviorHost/` and `android/behaviorHost/` adapt those cores to newline-delimited JSON on stdin/stdout; they are not part of the mobile apps. `tests/behavior/browse.test.mjs` runs the same scenarios on each host. It uses deterministic random selection and fixture media, storage outcomes, and sound preferences without accessing personal clips or preferences.

## Contract

Each line is one JSON request and gets one JSON response. `init` resets the browse node with `moves` (objects with unique string `id` and `name`, optionally boolean `imported`), an integer `randomIndex`, and boolean `soundEnabled`. An optional boolean `deleteSucceeds` controls the deletion fixture. `ls` returns the focused node, state and available actions. Browse `do` actions are `search` (`value`: string), `setSound` (`enabled`: boolean), `open` (`id`: string), `rollDice`, `clearChoice`, `requestDelete` (`id`), `cancelDelete`, and `confirmDelete`. A successful response has `ok: true`, `name`, `actions`, and `state`. `state.choice` is the selection emitted to the UI, not a live playback status. `restart` reconstructs the browse core using the same in-memory sound preference and library; this checks the preference boundary, not OS persistence. Invalid requests return `ok: false` and do not mutate the model.

For example, after running `./tests/behavior/run.sh`, explore the JVM host with:

```sh
android/behaviorHost/build/install/behaviorHost/bin/behaviorHost
```

Then enter one request per line:

```json
{"op":"init","moves":[{"id":"basic","name":"Basic step"},{"id":"turn","name":"Inside turn"}],"randomIndex":1,"soundEnabled":true}
{"op":"do","action":"search","value":"basic"}
{"op":"do","action":"rollDice"}
{"op":"ls"}
```

The dice returns `turn` with `loop: true` even though it is filtered out. Finish with Ctrl-D. The Swift host is compiled temporarily by the runner and uses the same protocol.

`initEditor` selects the editor node using a synthetic fixture: `duration` is a positive integer in milliseconds, `name` is the initial name, `sourceAvailable` optionally simulates source failure, and `saveSucceeds` controls the export result. Editor actions are `setStart`/`setEnd` (integer milliseconds), `setName`, `togglePreview`, `previewEnded`, `save`, `completeSave`, and `clearError`. The host records the core's export request in `lastExport` and dispatches the fixture result; `deferSave: true` delays it until `completeSave`. The apps pass that request to the native export service and dispatch its result. No real video is read or written.

`initPlayer` accepts `loop` and exposes `completed`, `close`, `leave`, and `failed` simulated media/lifecycle events. `initNavigation` exposes `openPlayer`, `openEditor`, and `close`. These initialize individual cores, not a single end-to-end screen tree; system picker presentation and the glue between cores are still platform-owned.

## Coverage and limits

Shared scenarios cover search, tile and full-library dice selection, empty libraries, sound preference reconstruction, navigation guards, playback completion/close/failure decisions, delete confirmation and outcomes, editor range clamping, preview, source failure, name validation, export requests and outcomes, and malformed actions. Media and delete results are simulated; the deterministic picker does not test randomness distribution.

Actual trim/export, imported clip files and OS preferences, playback and completion callbacks, lifecycle, thumbnails, accessibility, picker integrations, and interactions between cores and native screens are not exercised by these fixtures. In particular, a save does not add a fixture move to a live browse screen: a full import-to-grid journey is not yet covered. Continue device and simulator checks as described in the README. When extending coverage, wire actions through code used by the real app; avoid implementing a separate test-only version of behavior. Extend the protocol and tests on both platforms together.
