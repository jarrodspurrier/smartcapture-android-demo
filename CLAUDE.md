# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Focused Android demo of the **GBG SmartCapture Document** module plus the **GBG IVS (Identity Verification Suite)** integration against `https://ditto.gbg.com`. Single-activity Jetpack Compose app that runs the full four-step flow end-to-end: create session → capture front + back of an ID → submit images → poll for status → render result.

Integration contract lives in `docs/android-ivs-integration-guide.html` — that guide is the source of truth for endpoints, payloads, polling cadence, error handling, and the hardcoded constants (`sensorType=Mobile`, `faceMatchEnabled=false`, etc.). SmartCapture SDK reference: https://smartcapture-docs.idscan.cloud/docs/android/intro.

## Build and run

Gradle wrapper is checked in. Common commands (run from repo root):

- `./gradlew assembleDebug` — build debug APK (`app/build/outputs/apk/debug/`).
- `./gradlew installDebug` — install debug APK on a connected device/emulator.
- `./gradlew assembleReleaseNoSigning` — release-configured build (minified) signed with the debug key; use this when you need a release-like build without the real keystore.
- `./gradlew assembleRelease` — true signed release; requires `appKeyStorePassword`, `appKeyAlias`, `appKeyPassword`, `appKeyPath` to be set as Gradle properties (defaults fall back to empty/`../key.jks`). The android-keystore repo must be checked out alongside this one.
- `./gradlew lint` — runs Android lint (`abortOnError=true`). `AutoboxingStateCreation` and `MutableCollectionMutableState` are disabled project-wide because they crash lint.
- `./gradlew test` / `./gradlew connectedAndroidTest` — no tests are written; the test dependencies are wired up but `app/src/test` and `app/src/androidTest` are absent.

### Configuration (`local.properties`)

- `ivsApiKey=<bearer token>` — required at build time to exercise the IVS flow. Baked into `BuildConfig.IVS_API_KEY_SEED` and seeded into Keystore-backed `EncryptedSharedPreferences` on first launch. Without it, the landing screen shows a "key missing" banner and Start is a no-op.
- `ivsBaseUrl=<url>` — optional override; defaults to `https://ditto.gbg.com`.

`app/build.gradle` reads these properties **manually** from `local.properties` — Gradle's `findProperty` does **not** look there, only at `gradle.properties` / command-line `-P`. If you add another `local.properties` value, follow the same pattern (`Properties().load(localPropertiesFile.inputStream())`).

### Private Maven repo

`settings.gradle` resolves SmartCapture SDK artifacts from `s3://maven-mobile-repo/{releases,snapshots}` and requires `awsAccessKey` and `awsSecretKey` Gradle properties (typically set in `~/.gradle/gradle.properties`). Builds fail at dependency resolution without them.

### SDK version coupling

Only `com.gbg.smartcapture:commons` and `com.gbg.smartcapture:documentcamera` are pulled in (other modules — mjcs / facecamera / ozone / ozoneui — were intentionally removed). When bumping the SDK, update both artifacts together — mismatched versions break at runtime, not compile time.

## Architecture

### Single-activity Compose
`RootActivity` (`app/src/main/java/com/gbg/smartcapture/bigmagic/activities/RootActivity.kt`) is the only activity. A minimal `NavHost` has exactly two routes — `Main` (the flow) and `Settings`. All flow screens (Landing, FlipDocument, Submitting, Polling, PollingExhausted, Terminal, Failed) are rendered by `MainRouter` based on the current `VerificationUiState`.

### Verification state machine (`VerificationUiState`)
`VerificationUiState` is a sealed hierarchy in `viewmodel/VerificationUiState.kt`. Transitions:

```
Idle ──onStart()──▶ CreatingSession ──IVS 201──▶ AwaitingFront
                                      IVS err──▶ Failed
AwaitingFront  ──onFrontCaptured──▶ FlipPrompt
FlipPrompt     ──onFlipContinue──▶ AwaitingBack
AwaitingBack   ──onBackCaptured──▶ Submitting
Submitting     ──submit OK──▶ Polling(attempt=1)
               ──submit err─▶ Failed
Polling        ──terminal status──▶ Terminal
               ──10× non-terminal─▶ PollingExhausted
               ──poll error───────▶ Failed
Terminal|Failed|PollingExhausted ──onReset()──▶ Idle
PollingExhausted ──onPollRetry()──▶ Polling(attempt=1)
```

Cancel on any capture state returns to `Idle` via `onCaptureCancelled()`.

### Capture trigger pattern — event, not state
`RootViewModel.captureRequests` is a `SharedFlow<CaptureSide>` with no replay. Entering `AwaitingFront` / `AwaitingBack` doesn't by itself start the camera; the VM also emits a one-shot `CaptureSide.FRONT` / `CaptureSide.BACK` event. `RootActivity.observeCaptureTriggers` collects that flow and launches `DocumentCameraActivity`.

This exists specifically because **`repeatOnLifecycle(STARTED)` replays the current `StateFlow` value** when the activity returns from the camera. If we drove launches off `verificationState` directly, we'd double-launch the camera every time. Don't regress to a state-based trigger.

### IVS network stack (`ivs/` package)
- `IvsConfig` — base URL + poll cadence (`POLL_INTERVAL_MS = 4000`, `POLL_MAX_ATTEMPTS = 10`) per the integration guide.
- `IvsModels` — Moshi DTOs (`@JsonClass(generateAdapter = true)` for forward-compat), `SessionStatus` enum with `isTerminal()`, sealed `IvsError`, `IvsResult<T>`, `PollEvent`.
- `IvsApi` — OkHttp wrapper, suspending `createSession` / `submitImages` / `getSession`. Attaches `Authorization: Bearer <key>` per call via the `apiKeyProvider` lambda. Maps HTTP codes to `IvsError`.
- `IvsRepository` — orchestrates the full flow. Holds the active `sessionId` in memory, handles 409 ("already submitted" → success), handles 400 session expiry (silent reseed + resubmit), and exposes `pollStatus()` / `retryPolling()` as `Flow<PollEvent>`.

Moshi uses **reflection** (`KotlinJsonAdapterFactory`) rather than codegen. `moshi-kotlin-codegen` via `kotlin-kapt` doesn't parse Kotlin 2.2 metadata (this project uses 2.2.21). Don't try to re-enable kapt codegen without first migrating to KSP.

### Secrets — Keystore, not SharedPreferences
`data/ApiKeyStore.kt` wraps `EncryptedSharedPreferences` backed by `MasterKey` (`AES256_GCM`, rooted in Android Keystore). `getOrSeedApiKey()` returns the cached key, or seeds from `BuildConfig.IVS_API_KEY_SEED` on first launch and caches it. This is a compliance requirement from the integration guide — plaintext SharedPreferences / DataStore for the API key is forbidden.

`data/LastSessionStore.kt` is a **separate** plain-`SharedPreferences` store for the most recent `sessionId` (not a secret). Keep the separation.

### ViewModel layer
`IRootViewModel` interface is implemented by `RootViewModel` (real, `AndroidViewModel`) and `MockedRootViewModel` (Compose previews). UI state surfaces as `StateFlow` / `SharedFlow`. When adding a new observable, wire it through both impls or previews will crash.

### Settings
`SettingsDataStore` wraps Jetpack DataStore Preferences. Active surfaces:
- `MANUAL_CAPTURE_TOGGLE` (boolean, default true) + `SettingsManualCaptureToggleDelayType` enum — feed into `DocumentScannerConfig.autoCaptureToggleConfig` (Hide / Show / ShowDelayed).
- `SHOW_CAPTURE_PREVIEW` (boolean, debug flag — not yet wired to UI).

To add a new setting: add to `SettingsSwitch` (or a new enum), plumb through `SettingsDataStore`, expose a `StateFlow` on `IRootViewModel`, and wire both concrete impls.

### Module layout
Single `:app` module. `build-common-config.gradle` holds the android block (SDK versions, signing configs, build types including `releaseNoSigning`, lint config, `buildFeatures.buildConfig=true`) and is applied from `app/build.gradle`. Put Android-level config changes there, not in the app module's build file. `compileSdk=36`, `targetSdk=35`, `minSdk=24`, JVM target `1.8`.

### Versioning
`deployVersionName` in the root `build.gradle` is a build timestamp; `deployVersionCode` is hardcoded to `1`. The `com.gbg.smartcapture:*` dependency version (currently `1.2.3`) is shown by `VersionNumberView`.

## Gotchas

- **App namespace is `com.gbg.smartcapture.bigmagic`.** The `bigmagic` segment is historical — don't "fix" it. Package paths on disk follow this.
- **`DocumentCameraSDK.init(DocumentCameraSDKConfig(...))` must run before any `DocumentCameraActivity` launch** — the native `smart_capture::init()` is invoked here. Called from `RootActivity.onCreate`. Without it, the native image analyzer aborts the process on first frame.
- **`DocumentCameraActivity.latestResult` is a consume-once getter** — the Kotlin companion getter reads the static field AND sets it to `null` as a side effect. Read it into a local variable *exactly once*; any second access returns `null`. Same applies to `latestMetadata`.
- **`INTERNET` permission is declared explicitly in `AndroidManifest.xml`.** When MJCS / Face / Ozone were removed, their manifest-merged permissions went with them. Removing this permission will crash OkHttp with a `SecurityException` on DNS lookup (a non-`IOException`, which OkHttp's dispatcher rethrows fatally — not via `Callback.onFailure`).
- **`local.properties` is read manually in `app/build.gradle`.** Gradle's `findProperty` does not look at `local.properties`.
- **JVM target is 1.8.** Don't introduce Java 11+ APIs. OkHttp 4.12.0 is pinned because newer majors require Java 11.
- **Lint abortOnError is on.** CI will fail on new lint warnings. `AutoboxingStateCreation` and `MutableCollectionMutableState` are force-disabled because they crash the lint tool itself.

## Debug tooling

The debug build exposes a `gbgdemo://poll` deep link that calls `GET /api/verification/sessions/{id}` without any UI typing:

```bash
# Poll the most recently created session (cached in LastSessionStore)
adb shell am start -a android.intent.action.VIEW -d "gbgdemo://poll" com.gbg.smartcapture.bigmagic

# Poll a specific session (e.g. a completed test session provided by GBG)
adb shell am start -a android.intent.action.VIEW -d "gbgdemo://poll?session=vs_xxx" com.gbg.smartcapture.bigmagic
```

The landing screen's debug panel (gated by `BuildConfig.DEBUG`) is pre-filled with the same cached id, so a manual poll is one tap.

Intent filter lives on `RootActivity` in `AndroidManifest.xml`; handler is `RootActivity.handleDeepLink`.

Useful logcat tags while debugging: `RootActivity`, `RootViewModel`, `IvsRepository`, `IvsApi`.
