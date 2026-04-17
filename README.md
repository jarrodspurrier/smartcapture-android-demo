# SmartCapture Android Demo

A small Android app that exercises the GBG **SmartCapture Document** SDK and the **GBG IVS** (Identity Verification Suite) REST API end-to-end: create session → capture front/back of an ID → submit → poll → render the result.

SmartCapture SDK reference: https://smartcapture-docs.idscan.cloud/docs/android/intro

## Quick start

### 1. AWS credentials (private Maven repo)

The SmartCapture SDK artifacts live in a private S3-backed Maven repo. Add your credentials to your **user** Gradle properties so they aren't checked in with the project:

`~/.gradle/gradle.properties`
```properties
awsAccessKey=YOUR_ACCESS_KEY
awsSecretKey=YOUR_SECRET_KEY
```

### 2. IVS API key

The demo needs a bearer token to call `https://ditto.gbg.com`. Drop it into the project's `local.properties`:

`local.properties`
```properties
ivsApiKey=YOUR_IVS_API_KEY
# Optional — override the default base URL:
# ivsBaseUrl=https://ditto.gbg.com
```

### 3. Build and install

With a device (or emulator) connected via `adb`:

```bash
./gradlew installDebug
```

Or to just build the APK without installing:

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## EULA

See the project EULA in [`LICENSE.md`](LICENSE.md).
