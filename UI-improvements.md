# Damoves UI/UX Redesign & Modernization Report

**Target Artifact:** `UI-improvements.md`  
**Application:** Damoves (Android native, Kotlin, Target SDK 37, Min SDK 26)  
**Date:** 2026-11-01  
**Audit Scope:** Grid view (`MainActivity`), Importer/Trimmer (`ImportActivity`), Fullscreen Player (`PlayerActivity`), Theme & Styles (`styles.xml`)

---

## Executive Summary & Product Vision

Damoves has a strong core promise: **let social dancers check a dance-move video in seconds between songs on a loud, dimly lit dance floor, and get right back to dancing.**

Currently, the interface presents as an unstyled developer prototype using raw Android platform widgets (`Button`, `EditText`, `Switch`, `GridView`, `AlertDialog`) instantiated programmatically with hardcoded hex values, uneven padding, and default Material 2/Holo-era styling. 

To feel like a polished, high-end consumer product, Damoves should embrace **Material Design 3 (M3)** with custom dark tokens tailored to dimly lit club and ballroom environments. Material 3 (Material You) is Google's latest native design system and element library. We will add the official Google Material Components library (`com.google.android.material:material`) alongside AndroidX RecyclerView and testing libraries, and pair the UI overhaul with automated UI/flow tests for every interaction.

---

## Required Native Libraries & Dependencies

To support modern native M3 elements, smooth list performance, and robust automated UI flow tests, we need to update `app/build.gradle` from its bare `junit:4.13.2` configuration to include:

```groovy
dependencies {
    // Material 3 Native Design System & Views
    implementation "com.google.android.material:material:1.12.0"
    implementation "androidx.recyclerview:recyclerview:1.4.0"

    // Unit Testing
    testImplementation "junit:junit:4.13.2"
    testImplementation "org.robolectric:robolectric:4.14.1"
    testImplementation "androidx.test.ext:junit:1.2.1"
    testImplementation "androidx.test:core:1.6.1"

    // Automated UI & User Flow Testing (Espresso / AndroidX Test)
    androidTestImplementation "androidx.test.ext:junit:1.2.1"
    androidTestImplementation "androidx.test:runner:1.7.0"
    androidTestImplementation "androidx.test:rules:1.6.1"
    androidTestImplementation "androidx.test.espresso:espresso-core:3.7.0"
    androidTestImplementation "androidx.test.espresso:espresso-contrib:3.7.0"
}
```

*Espresso version note:* Espresso 3.6.1 (and earlier) reflects `android.hardware.input.InputManager.getInstance()`, which no longer exists on API 37 system images — every `onView()` call throws `NoSuchMethodException` there. Espresso 3.7.0 uses `Context.getSystemService(InputManager.class)` on API 23+ and is the minimum for testing on API 37 devices.

*Note on Material 3:* Google provides M3 via two paths:
1. **Material Components for Android** (`com.google.android.material:material`) for XML/View-based apps like Damoves. This provides native M3 widgets (`MaterialCardView`, `SearchBar`, `MaterialButton`, `MaterialAlertDialogBuilder`, `BottomSheetDialog`) that drop directly into our existing View hierarchy without full rewrite.
2. **Jetpack Compose Material 3** (`androidx.compose.material3`) for declarative Compose apps. Because Damoves is built around native Android platform views and `VideoView`, Material Components is the zero-friction, production-tested choice.

---

## Current Architecture vs. Modern Native Architecture

| Surface | Current Implementation | Modern Native Target (M3 / Platform Views) |
|---|---|---|
| **Theme / Design System** | `@android:style/Theme.Material.NoActionBar` with hardcoded raw hex colors in Kotlin (`Color.rgb(16, 17, 20)`) | `Theme.Material3.Dark.NoActionBar` (or custom M3 token palette in `colors.xml` / `styles.xml`) |
| **Move Grid** | Legacy `GridView` (API 1) with custom `BaseAdapter` and manual recycling | `RecyclerView` with `GridLayoutManager`, diffing, and item animations |
| **Search Field** | Bare `EditText` with `roundedBackground` drawable, no clear button or search icon | M3-style `SearchBar` / anchored `SearchView` or elevated text field with trailing clear action and leading icon |
| **Header Actions** | Horizontal `LinearLayout` cramming Title, generic `Button("Add clip")`, and legacy `Switch` | Clean App Bar with title, icon buttons (`Add`, `Mute/Unmute`), and subtitle/badge indicator |
| **Move Cards** | `FrameLayout` with raw `ImageView` + translucent `TextView` bottom banner | M3 `MaterialCardView` with subtle border, aspect ratio containment, smooth gradient overlay, and `headlineSmall`/`labelLarge` typography |
| **Importer Layout** | `ScrollView` wrapping raw `Button` rows with monospace-ish ASCII text (`−5s`, `+5s`, `START −`) | Bottom sheet / docked panel with M3 SegmentedButtons, elevated action buttons, and styled slider controls |
| **Timeline Scrubber** | Custom `ClipTimeline` with flat cyan line and small circles | High-contrast dual-thumb range slider with haptic ticks, millisecond time bubbles, and thumbnail strip preview |
| **Dialogs & Overlays** | Default system `AlertDialog` with plain text items | M3 `ModalBottomSheet` or styled `MaterialAlertDialogBuilder` with icons and descriptive helper copy |
| **Tactile / Motion** | System clicks only, no feedback on nudge or seek | System haptics (`HapticFeedbackConstants.CLOCK_TICK` / `CONFIRM`), smooth view transitions |

---

## Heuristic & Design Lens Evaluation

| Lens | Score (/10) | Evaluation & Key Observation |
|---|---|---|
| **1. First Impression** | 4/10 | Utilitarian and sparse. Looks like an internal test harness rather than a specialized dancer's companion. |
| **2. Visual Hierarchy** | 5/10 | The header competes with search; the "Add clip" button has equal weight to primary navigation; cards lack elevation or edge contrast. |
| **3. Color & Contrast** | 5/10 | Colors are hardcoded (`#101114`, `#1D1F25`, `#2B2D34`). No semantic palette for primary, secondary, surface variants, or danger states. |
| **4. Typography Voice** | 4/10 | Uses system default sans-serif at generic sizes (`16f`, `20f`). Missing proportional line-heights, tracking, and weight contrast. |
| **5. Interaction & Haptics** | 4/10 | Buttons lack proper ripple masks and state feedback; hold-to-repeat has no haptic response; deletion uses a generic alert box. |
| **Total Score** | **22/50** | **Needs architectural redesign and tokenization.** |

---

## Detailed Findings & Priority Action Items

### P0 (High Severity): Ergonomics & Visual Usability

#### 1. Header Cramming & Reachability
* **Location:** `app/src/main/java/com/example/soundboard/MainActivity.kt:45-115`
* **Current State:** Header places Title, "Add clip" button, and "Sound on" toggle inside one horizontal row above the search bar. On narrow devices or large font scaling, "Add clip" and the Switch truncate or collide.
* **Proposed Redesign:**
  * Adopt a standard **Top App Bar** pattern:
    * Left: App Brand (`Damoves`, 22sp bold, tracking tight).
    * Right: Action icons with standard 48×48dp touch targets (`R.drawable.ic_volume_up` / `ic_volume_off` toggle button, and `R.drawable.ic_add` icon button).
  * Place the Search input directly below the top bar as an edge-to-edge floating search pill (`48dp` height, container color `#1E2028`, corner radius `24dp`).

#### 2. Card Typography & Contrast Legibility
* **Location:** `app/src/main/java/com/example/soundboard/MainActivity.kt:222-249`
* **Current State:** Move name sits on an artificial black overlay (`Color.argb(210, 16, 17, 20)`) with hard rectangular cutoff at the bottom third of the card.
* **Proposed Redesign:**
  * Replace the solid black rectangle with a vertical scrim gradient (`#00000000` to `#D90B0C10`) covering the bottom 50% of the card.
  * Use bold, condensed sans typography (`15sp`, medium weight, white text with `0.5dp` subtle shadow) with 2-line max ellipsis.
  * Add a tiny chip/badge in the top-right corner of imported moves (`"Added"` or a subtle icon) so users instantly distinguish personal clips from bundled clips.

#### 3. Importer Control Density & Visual Hierarchy
* **Location:** `app/src/main/java/com/example/soundboard/ImportActivity.kt:110-175`
* **Current State:** A dense stack of four rectangular gray buttons (`START −`, `START +`, `END −`, `END +`) above three rectangular gray buttons (`−5S`, `PAUSE PREVIEW`, `+5S`), all with uppercase text and identical gray backgrounds (`#DD000000` panel).
* **Proposed Redesign:**
  * Group controls into visual tiers:
    1. **Timeline & Range Bubble:** Show timestamps as formatted badges (`00:04.2 / 00:18.5`) above a customized scrubber.
    2. **Playback Row (Center):** Large primary floating Play/Pause button (circular, 56dp, accent tinted `#A6B4D6`) flanked by secondary pill buttons for `−5s` and `+5s`.
    3. **Fine Trim Segmented Control:** Two pill groups for Start and End with clear icons (`[−0.5s] Start [+0.5s]` and `[−0.5s] End [+0.5s]`).
    4. **Action Area:** Floating full-width primary button for "Save Move" pinned above system navigation insets.

---

### P1 (Medium Severity): Modern Native Android Elements

#### 4. Replace GridView with RecyclerView + DiffUtil
* **Problem:** `GridView` cannot do item animations, forces full dataset reloads on item deletion, and re-renders visible tiles awkwardly.
* **Solution:**
  * Migrate to `androidx.recyclerview.widget.RecyclerView` with `GridLayoutManager(3)`.
  * Use `ListAdapter` with `DiffUtil.ItemCallback<Move>` so deletion smoothly animates the card collapsing and neighboring cards reflowing.

#### 5. Replace AlertDialog with Modal Bottom Sheet for Tile Actions
* **Problem:** Legacy `AlertDialog` displays small centered system popups with stark white/gray themes that blind users in dark dance halls.
* **Solution:**
  * When long-pressing an imported tile, open a **Dark Modal Bottom Sheet**:
    * Header with thumbnail preview and move name.
    * Action 1: "Edit trim & name" (with `ic_edit` icon).
    * Action 2: "Delete from Damoves" (with `ic_delete` icon, styled in destructive red `#FF897D`).

#### 6. Micro-Interactions & Haptic Feedback
* **Problem:** Operating controls on a noisy dance floor provides no tactile confirmation when adjustments occur.
* **Solution:**
  * Trigger `view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)` on every 0.5s nudge and hold-to-repeat interval.
  * Trigger `view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)` on successful clip save and clip deletion.
  * Add a subtle ripple (`RippleDrawable`) bounded by card outline for touch feedback.

---

### P2 (Polish & Delight): Motion & Ambient Polish

#### 7. Floating Action Button (FAB) for Dice
* **Current State:** `ImageButton` with manual circle `GradientDrawable` and hardcoded elevation.
* **Redesign:** Use native M3 Floating Action Button styling with smooth press scale animation (interpolator `FastOutSlowInInterpolator`), anchored at bottom-end with proper window inset margins (`16dp` from right, `16dp` above bottom navigation bar).

#### 8. Empty Search & Zero-State Experience
* **Current State:** When a search query matches nothing, the screen is a blank dark rectangle.
* **Redesign:** Show a clean centered zero-state illustration or icon (`ic_search_off`), "No moves found for '...'", and a "Clear search" text button.

---

## Tokenized Dark Palette (OLED-Friendly M3 Palette)

```
Surface Base (Canvas):       #0C0D10  (Deep OLED charcoal, not harsh pure black)
Surface Container (Cards):   #181920  (Raised surface)
Surface Container High:      #22242E  (Modals, bottom sheets, search bar)
Outline / Divider:           #2E313D  (1dp crisp separation)

Accent Primary:              #A6B4D6  (Soft peri-indigo: high contrast, low glare)
Accent On-Primary:           #141E34  (Deep contrast text on primary buttons)
Accent Container:            #2D3852  (Subtle active state pills)

Text Primary:                #F1F3F9  (92% high-legibility white)
Text Secondary:              #9FA3B2  (Muted metadata, timing)
Danger / Destructive:        #FF897D  (Soft coral red for Delete actions)
```

---

## Implementation Roadmap (Step-by-Step)

### Phase 1: Foundation & Theme Tokens
1. Introduce semantic color and dimension resources in `app/src/main/res/values/colors.xml` and `dimens.xml`.
2. Update `styles.xml` to inherit clean dark window flags and standard typography styles.
3. Extract reusable drawable backgrounds (pill buttons, search container, card gradient scrim).

### Phase 2: Main Grid & Search Modernization
1. Refactor `MainActivity` layout: Replace manual `LinearLayout` header with dedicated App Bar and Search container.
2. Upgrade card tile view: Apply vertical scrim gradient and improved typography.
3. Replace `GridView` with `RecyclerView` + `GridLayoutManager`.
4. Replace long-press `AlertDialog` with custom Bottom Sheet dialog.

### Phase 3: Clip Importer & Scrubber Redesign
1. Re-organize `ImportActivity` bottom panel into clear sections: Timeline, Scrub/Playback, Trim Controls, Save.
2. Refactor `ClipTimeline` drawing: Add active range fill, thumb indicators, and time indicators.
3. Add haptic feedback on button nudges and scrubber dragging.

### Phase 4: Polish & Empty States
1. Add animated empty search state.
2. Polish FAB dice button motion and pressed states.
3. Run device verification across rotation and accessibility text zoom.

---

## Automated UI & User Flow Testing Suite

To ensure high-fidelity UI and eliminate manual testing friction, every flow documented in `docs/userflows.md` will have automated UI tests using **Espresso** (on emulator/device) and **Robolectric** (for fast local JVM UI tests).

### Matrix of Automated User Flow Tests

| Flow / Screen | Test Target (`app/src/androidTest/...`) | Verification Criteria |
|---|---|---|
| **Flow 1: Grid Browsing & Search** | `BrowseAndSearchFlowTest` | - Grid populates all moves with card dimensions<br>- Typing in search filter updates visible cards instantly<br>- Empty search shows zero-state container with "Clear" action<br>- Query state survives device rotation |
| **Flow 2: Tile Playback** | `TilePlaybackFlowTest` | - Tapping card launches player with aspect ratio preserved<br>- Completing single-play video returns automatically to grid<br>- Tapping screen early cancels playback and returns immediately<br>- Mute preference persists and applies to player volume |
| **Flow 3: Dice Random Loop** | `DiceLoopFlowTest` | - Tapping floating dice picks a move even when search filter is active<br>- Video repeats infinitely until explicitly tapped or dismissed<br>- Back/Home immediately releases playback resources |
| **Flow 4: Clip Importer & Scrubber** | `ClipImportFlowTest` | - Opening document picker intent hands off clip to trimmer<br>- Scrubbing updates timestamp badges and video position<br>- `−5s` and `+5s` buttons scrub playhead accurately<br>- Press-and-hold Start/End buttons repeats adjustment and stops cleanly on release<br>- Saving clip stream-copies video, disables button with "Saving…", and adds move to grid |
| **Flow 5: Manage Imported Clips** | `ManageImportedClipsFlowTest` | - Long-pressing imported tile opens action menu (Edit / Delete)<br>- Long-pressing bundled move is inert (does not trigger player or menu)<br>- "Edit clip" re-opens importer with prefilled name<br>- "Delete" prompts confirmation dialog; confirming removes file from disk and animates tile out of grid |

### Running the Flow Suite

```sh
# Fast JVM UI component tests without emulator
./gradlew :app:testDebugUnitTest

# Full on-device / emulator Espresso user flow test pass
./gradlew :app:connectedDebugAndroidTest
```
