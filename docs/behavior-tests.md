# Headless behavior tests

[Shopify's Reflex article](https://shopify.engineering/reflex) describes an architecture, not a published library. Damoves implements a small subset: a UI-independent browse model per platform, an action/state interface, deterministic fixtures, and one suite that runs against both models. There is no Shopify dependency and no introspection server in either app.

## Run

On macOS, install Xcode, Node.js 18 or later, and JDK 25. Set `JAVA_HOME` to a JDK 25 installation. The headless suite does not require the Android SDK; Android app builds still do. From the repository root:

```sh
./tests/behavior/run.sh
```

The script compiles the Swift host into a temporary directory and builds the JVM-only Gradle `behaviorHost` distribution. Gradle may download dependencies on the first run. Tests themselves use only in-process fixtures and do not intentionally access the network; there is currently no network-blocking guard. A simulator or emulator is not needed.

GitHub Actions runs this same command on a macOS runner for pushes and pull requests; it can also be started manually from the Actions tab. The workflow is `.github/workflows/behavior-tests.yml`. It does not build the mobile apps or run their separate unit or device tests.

`ios/Damoves/BrowseCore.swift` and `android/app/src/main/java/com/example/soundboard/BrowseCore.kt` are the actual implementations used by the browse screens. `ios/BehaviorHost/` and `android/behaviorHost/` adapt those models to newline-delimited JSON on stdin/stdout; they are not part of the mobile apps. `tests/behavior/browse.test.mjs` runs the same journeys on each host. It uses deterministic random selection and explicit fixture moves/sound state, without accessing personal clips or preferences.

## Contract

Each line is one JSON request and gets one JSON response. `init` resets the session with `moves` (objects with unique string `id` and `name`), an integer `randomIndex`, and boolean `soundEnabled`. `ls` returns the browse node, state and available actions. `do` dispatches `search` (`value`: string), `setSound` (`enabled`: boolean), `open` (`id`: string), `rollDice`, or `clearChoice`. A successful response has `ok: true`, `name: "Browse"`, `actions`, and `state`. `state.choice` is the selection emitted to the UI, not a live playback status. Invalid requests return `ok: false` and do not mutate the model.

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

## Coverage and limits

The shared journeys cover case-insensitive partial search, whitespace, no matches, a visible tile's one-shot selection, rejection of hidden tiles, dice selection from the full library, empty libraries, sound state, and malformed actions. They exercise browse decisions, not rendering or native playback. The deterministic picker tests the selected index and looping flag, not randomness distribution. Sound persistence is provided by platform storage and is not tested by the headless fixture.

The importer, trim/export, imported clip storage/deletion, actual audio/video playback and completion, lifecycle, thumbnails, accessibility, and picker integrations have not been moved into the headless core. Continue to test these on devices or simulators as described in the README. When adding journeys, wire actions through code used by the real app; avoid implementing a separate test-only version of the behavior. Extend the protocol and tests on both platforms together.
