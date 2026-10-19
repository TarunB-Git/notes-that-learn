# Build and contribute

## Android

Use JDK 17, Android SDK platform 35, and build tools 35.0.0. Open `android/`
in Android Studio, or configure `JAVA_HOME` and `ANDROID_HOME` for the wrapper:

```bash
cd android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
```

Debug output: `android/app/build/outputs/apk/debug/app-debug.apk` from the repo
root. `:app:installDebug` installs it on a connected device. Default builds target
ARM64 / Android 11+. Add `-Pemulator=true` for x86_64 emulator libraries.

For the Google-free distribution, use `-Pfdroid=true` on every Gradle invocation.
Clean before switching distributions because they share `app/build`:

```bash
./gradlew :app:clean -Pfdroid=true
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assemblePreview -Pfdroid=true
```

Preview output: `app/build/outputs/apk/preview/app-preview.apk` from `android/`.
**Note Chat Preview** uses `dev.notechat.preview` and a debug key, installing
beside the production app. It cannot validate a production upgrade.

## Browser and desktop

From the repository root, with Node.js 22+:

```bash
node showcase/tests/classifier.test.mjs
node showcase/build.mjs
node --check desktop/src/note_processing/web/app.js
```

After the [desktop installation](desktop/README.md):

```bash
python -m unittest discover -s desktop/tests
```

The [checks workflow](.github/workflows/checks.yml) covers both Android builds,
desktop, and the browser demo. Device-only smoke checks are implemented in
[RuntimeChecks.kt](android/app/src/androidTest/java/dev/notechat/RuntimeChecks.kt).

## Contributions

Keep changes focused on a user workflow and describe the validation. Domain
operations live in `Domain.kt`; only confirmed labels may teach the classifier.
[Bug reports](https://github.com/TarunB-Git/note-processing/issues/new?template=bug_report.yml)
should use invented examples without personal notes.

[Website](showcase/README.md) · [App design](README.md#engineering-worth-exploring)

Current app/desktop code: [Apache 2.0](LICENSE), with [notices](NOTICE).
Historical `archive/v1/` has separate licensing information.
