# Build and contribute

Start with the [project overview](README.md), [engineering case study](docs/engineering.md),
and [known release gaps](docs/release-readiness.md). The main application is
Android; the desktop organizer has a separate [quick start](desktop/README.md).

## Build Android

Requirements:

- JDK 17.
- Android SDK platform 35 and build tools 35.0.0.
- An ARM64 phone running Android 11 or later for the standard APK.

Clone the repository:

```bash
git clone https://github.com/TarunB-Git/note-processing.git
cd note-processing
```

Open `android/` in Android Studio. Select JDK 17 for Gradle and install the SDK
packages above. For command-line builds, configure `JAVA_HOME` and
`ANDROID_HOME` for those installations, or supply `sdk.dir` in the ignored
`android/local.properties` file. Gradle downloads dependencies on the first run.
Use the included wrapper:

```bash
cd android
./gradlew :app:assembleDebug
```

The APK is `android/app/build/outputs/apk/debug/app-debug.apk`, relative to the
repository root. With a connected development device:

```bash
./gradlew :app:installDebug
```

For a minified build using the debug signing key:

```bash
./gradlew :app:assemblePreview
```

Its output is `android/app/build/outputs/apk/preview/app-preview.apk`. It is useful
for investigating shrinking/packaging problems, but its signing key differs
from production. It cannot validate an upgrade over the published signed app.

## Run the checks

From `android/`:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

From the repository root, after installing the desktop package in a virtual
environment:

```bash
python -m unittest discover -s desktop/tests
node --check desktop/src/note_processing/web/app.js
```

The [checks workflow](.github/workflows/checks.yml) runs these checks. Android
unit tests include Robolectric tests with Android resources. Reports appear in
`android/app/build/reports/tests/testDebugUnitTest/` and
`android/app/build/reports/lint-results-debug.html`.

## Device smoke checks

Use a disposable emulator or test installation. `RuntimeChecks` can create
sample notes in an empty installation and is not intended for a personal
workspace. For an x86_64 emulator, include its ABI when building:

```bash
cd android
./gradlew -Pemulator=true :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w dev.notechat.test/dev.notechat.RuntimeChecks
```

This checks Keystore-backed encrypted storage, tamper rejection, and a basic
launch/share path. It does not establish compatibility for every native library
or replace the real-device matrix in the [readiness report](docs/release-readiness.md).
The ordinary build contains ARM64 libraries only; `-Pemulator=true` also packages
x86_64 for an emulator.

## Make a useful change

Keep a change focused on one user workflow. Describe the problem, the behavior
after the change, and the checks or manual steps that establish it. Domain
operations belong in `Domain.kt`; persistence and Markdown reconciliation have
separate modules. Avoid teaching the classifier from unconfirmed predictions.

For a bug, a short synthetic note and reproducible steps are more useful than
a personal database. [Open a bug report](https://github.com/TarunB-Git/note-processing/issues/new?template=bug_report.yml)
with the installed version, Android version, expected behavior, and actual
result. Omit personal notes, recordings, backup passwords, and signing material.

The [readiness findings](docs/release-readiness.md#fix-before-expanding-the-beta)
identify the next reliability work. Discuss a major new feature in an issue
before implementing it so its interaction with existing storage and learning
rules is clear.

## Release process

The [signed release workflow](.github/workflows/release.yml) runs on `vX.Y.Z`
tags. It checks the code, builds a signed APK and AAB, verifies the package
version and signatures, generates checksums, and creates a **draft** GitHub
release.

Maintainers must update `versionName` and increment `versionCode` in
`android/app/build.gradle.kts`. The tag must match `versionName`. Commit every
intended source file before tagging; local-only files do not reach CI.

Signing uses the repository's existing secrets:
`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and
`ANDROID_KEY_PASSWORD`. For local signing, the build accepts
`ANDROID_KEYSTORE_PATH` with the three password/alias environment values, or the
Android Studio injected signing properties. Never add signing keys or passwords
to source control.

Validate the signed update and recovery workflows before publishing a draft.
Publishing a GitHub release and submitting to Google Play are separate steps;
the [readiness report](docs/release-readiness.md#later-public-google-play-release)
records the remaining Play preparation.

## License

The current application and desktop organizer use [Apache 2.0](LICENSE), subject
to the [third-party notices](NOTICE). Historical material in `archive/v1/` is
excluded from that license grant; inspect its own documentation before reuse.
