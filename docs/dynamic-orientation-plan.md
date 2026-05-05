# Enable dynamic portrait/landscape orientation

## Context

Today the app is locked to portrait at the manifest level (`app/src/main/AndroidManifest.xml:23`). The user is asking whether the app can rotate with the device instead. Yes — this is a small, targeted change. The main considerations are (1) RootActivity is the only activity we own, (2) the Compose layouts were authored assuming portrait and have no landscape-specific resources, and (3) `DocumentCameraActivity` is from the external GBG SmartCapture SDK, so its rotation behavior is not ours to set.

## Recommended approach

Let the system follow the device sensor for `RootActivity`, and handle the configuration change in-process so state isn't churned on every rotation.

### Change 1 — AndroidManifest.xml

File: `app/src/main/AndroidManifest.xml` (activity block at lines 19–39)

- Replace `android:screenOrientation="portrait"` with `android:screenOrientation="fullUser"`. `fullUser` respects the user's OS-level auto-rotate setting (portrait, landscape, reverse-landscape, reverse-portrait) — the most user-friendly default. Alternatives if we want different semantics:
  - `unspecified` — let the system pick, usually matches sensor
  - `fullSensor` — always follows sensor regardless of user's auto-rotate toggle
  - `sensor` — sensor-driven but excludes reverse-portrait
- Add `android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"` so rotation does **not** destroy & recreate `RootActivity`. Compose recomposes on configuration change and re-reads `LocalConfiguration.current` automatically, so no code path is needed to restore state across recreation.

Why `configChanges` matters here specifically: the capture-trigger contract in `RootViewModel.captureRequests` is a `SharedFlow` with `replay=0` (CLAUDE.md calls this out under "Capture trigger pattern"). If we let the activity recreate on rotation, a mid-flight capture event could race with the collector tear-down. Declaring `configChanges` sidesteps that class of bug entirely and is the idiomatic Compose approach.

### Change 2 — spot-check Compose layouts for landscape

No code changes required up-front, but visually verify these screens on a rotated device since none of them have landscape-specific resources:

- `compositions/AppSelectionViewCompose.kt` (landing)
- `compositions/FlipDocumentViewCompose.kt`
- `compositions/SubmittingViewCompose.kt`
- `compositions/PollingViewCompose.kt`
- `compositions/PollingExhaustedViewCompose.kt`
- `compositions/VerificationResultViewCompose.kt`
- `compositions/FailedViewCompose.kt`
- `compositions/SettingsViewCompose.kt`

Most are simple centered `Column`/`Box` layouts and should render acceptably in landscape. If any screen clips or looks cramped (for example the polling screen's stacked vertical content on short landscape heights), wrap the root in a `verticalScroll(rememberScrollState())` or branch on `LocalConfiguration.current.orientation`. Defer this work until we actually see a problem on device — don't pre-optimize.

### What is intentionally out of scope

- **`DocumentCameraActivity`** — it ships from `com.gbg.smartcapture:documentcamera` and is not declared in our manifest. Whatever orientation policy the SDK picks is what we get. Do not try to wrap or override it.
- **Landscape-specific resource folders** (`layout-land/`, `values-land/`) — not needed with Compose; orientation-dependent styling belongs inside composables reading `LocalConfiguration`.

## Critical files

- `app/src/main/AndroidManifest.xml` — the only edit required to make rotation work.

## Verification

1. `./gradlew installDebug` to deploy to the connected ET45.
2. Confirm the device's system auto-rotate toggle is on.
3. Launch the app and rotate the tablet through portrait → landscape → reverse-landscape; confirm the landing screen rotates and that the "Start" button, missing-key banner (if applicable), and debug panel (debug build only) remain usable.
4. Tap **Start**, go through front capture, return to `FlipPrompt`, rotate, confirm the flip prompt still displays correctly and the flow continues.
5. Complete a full front + back capture → submit → poll → terminal result, rotating at least once mid-flow, and confirm state is preserved across rotations (it should be — `configChanges` prevents recreation, and the ViewModel holds `VerificationUiState` regardless).
6. Observe `DocumentCameraActivity` during capture — note (but do not try to fix) whatever the SDK does with rotation. Report behavior back so we can decide if anything further is needed.
