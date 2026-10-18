# F-Droid release candidate — 18 October 2026

Candidate: **1.5.1 / versionCode 9**. Local files only. No tag, public release,
F-Droid request, or production-signed phone result exists yet.

## Implemented

- Separate free build: `-Pfdroid=true`. Google translation/OCR SDKs, source,
  notices, settings and message actions are excluded. Imported preferences cannot
  invoke them. Notes, local classification, optional speech/MiniLM, media,
  reminders and widgets remain available.
- ZIP export now enforces the reader's 16 MiB workspace, 4,998-attachment and
  512 MiB limits, verifies actual attachment lengths, and deletes failed output.
  An exactly-at-limit workspace round-trips in regression coverage.
- Restore validates the full merged state before importing media or replacing
  notes. Pre-promotion notebook/category identity conflicts are rejected with a
  recovery explanation. Current notes and further backups remain usable.
- Folder sync checks all generated documents and workspace metadata against the
  16 MiB reader limit before writing; an oversized workspace preserves the last
  recoverable folder and restores the previous engine state.
- ONNX Runtime 1.31.0 replaces 1.20.0. The packaged ARM64 libraries and APK ZIP
  alignment pass 16 KiB checks. Real-device MiniLM inference is still required.
- Build tools pinned to 35.0.0; AGP 8.6.1, Gradle 8.9 and Kotlin 2.0.21 remain
  pinned. Checkout-dependent VCS metadata is excluded from release APKs. Native libraries are packaged unchanged, so a
  locally installed NDK/strip tool cannot change their bytes.
- `dev.notechat.preview`, labelled **Note Chat Preview**, installs beside the
  production app. Its debug signature does not establish production upgrades.
- Release CI prepares standard APK/AAB plus a separately named, signed free APK.
  The local signing helper checks the public certificate fingerprint against the
  actual published v1.5.0 APK. A wrong-key test rejects output and cleans up.
- Listing text, icon, current native UI renders from the free Robolectric build,
  code-9 changelog, build recipe and an
  inclusion-request draft are prepared. The recipe stays Disabled until the
  release/tag/signature prerequisites exist. Automatic updates remain disabled.

Optional WhatsApp forwarding and user-selected Drive backup destinations are
explicitly disclosed as NonFreeNet; core notes and classification work offline.
No proprietary SDK is bundled in the free APK. The Drive picker is a backup
save destination, not built-in automatic cloud sync.

## Verification record

Final results and artifact hashes are in
[the verification record](../distribution/fdroid/verification.txt), also copied
to `dist/fdroid-candidate/verification.txt`. They cover this checkout, not
published v1.5.0.

F-Droid tools used locally: fdroidserver 2.4.5, refreshed SUSS signatures,
source scanning of a copy excluding `android/google` and `archive/v1`, APK
scanning, and metadata lint using current official category/anti-feature keys.
The upstream category icons were omitted from the local validator config;
category names and rules were retained. The recipe was linted with Disabled
removed only in a temporary validation copy. This does not publish anything.

A clean source copy without Google code can build unsigned through Gradle.
Compare its APK with the original checkout, byte for byte. This local comparison
is useful evidence; F-Droid must independently rebuild the actual release and
reproduce its signature. A full F-Droid build-server VM was not run here.

## Phone handoff

Owner reports checks passed on Samsung / Android 16. Exact model, tested APK
and production-signed upgrade remain unspecified. The later mode toggle,
autosave and appearance changes require a focused retest. See the ordered
[release handoff](release-next-steps.md).


1. Install `dist/fdroid-candidate/note-chat-v1.5.1-fdroid-preview.apk` on an ARM64
   Android 11+ phone. It is a separate app and cannot replace `dev.notechat`.
2. With test notes, check capture/edit/delete/restore, category correction, both
   layouts, recording/playback, an image attachment, the widget and a reminder.
   Deny a permission, reopen the app, then allow it and retry.
3. Download optional MiniLM and English speech models, then enable airplane mode
   and verify classification/transcription. Report the phone and Android version;
   if possible include a Google-free device and a 16 KiB-page device.
4. Export an encrypted ZIP. Restore it into an empty test installation, compare
   notes/media/labels, and try a wrong password. Use only disposable preview data
   when clearing its storage. Connect a Markdown folder, edit a note externally,
   and reopen/sync to confirm recovery.
5. Sign the unsigned release locally with the existing production key:

```bash
ANDROID_HOME=/path/to/Android/Sdk bash distribution/fdroid/sign-release.sh \
  dist/fdroid-candidate/note-chat-v1.5.1-fdroid-unsigned.apk \
  dist/fdroid-candidate/note-chat-v1.5.1-fdroid-signed.apk
```

The helper prompts locally for keystore path/alias; apksigner prompts for its
passwords. Existing `ANDROID_KEYSTORE_*`, `ANDROID_KEY_ALIAS` and
`ANDROID_KEY_PASSWORD` release environment variables are also supported. Keep
keys/passwords out of chat, source control and public requests.

6. Keep an independent copy of the current workspace/backup, then install the
   signed `dev.notechat` candidate over published v1.5.0. Confirm notes, media,
   approvals, settings and app-lock behavior survive. Never uninstall the
   production app just to resolve a signature mismatch.

Record pass/fail, exact device model, Android version, APK and any failed step.
The owner report is manual evidence; no connected-device outcome was observed
by the preparation tools.

## Submission after phone acceptance

Commit all intended sources/listing assets, tag that reviewed commit `v1.5.1`,
and publish the verified release. Do not reuse v1.5.0 / code 8. The release
workflow will produce `note-chat-v1.5.1-fdroid-arm64.apk` using the same production
key. Its public certificate SHA-256 is recorded in
`distribution/fdroid/production-cert-sha256.txt`.

Verify F-Droid can reproduce that exact free APK and signature, then remove
Disabled from `distribution/fdroid/dev.notechat.yml`. Submit its build metadata
or use `distribution/fdroid/submission.md` for the inclusion request. Respond to
maintainer review; acceptance and timing are theirs. Do not point the recipe at
the standard Google-enabled APK or give anyone the private keystore.

The user authorized proceeding with F-Droid on 18 October. Signing and phone
acceptance are the outstanding owner actions; no repeat publication permission
is needed for the already-authorized F-Droid work once those prerequisites pass.

## Commands

From `android/`, with JDK 17 and ANDROID_HOME configured:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug -Pfdroid=true --no-daemon --max-workers=1
./gradlew :app:assemblePreview :app:assembleRelease -Pfdroid=true --no-daemon --max-workers=1
python3 ../distribution/fdroid/check-apk.py app/build/outputs/apk/release/app-release-unsigned.apk
python3 ../distribution/check-native-libraries.py app/build/outputs/apk/release/app-release-unsigned.apk
```

The normal build omits `-Pfdroid=true`; both use `app/build`, so copy artifacts
before switching distributions. For a release, clean `:app:clean -Pfdroid=true`
after copying standard artifacts and before building the free APK. The release
workflow does this to keep compilation outputs separate. Preview has the debug key; release is unsigned
until signed through the helper or established release workflow.

[Inclusion policy](https://f-droid.org/en/docs/Inclusion_Policy/) ·
[Submission guide](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/) ·
[Reproducible builds](https://f-droid.org/en/docs/Reproducible_Builds/) ·
[Listing format](https://f-droid.org/en/docs/All_About_Descriptions_Graphics_and_Screenshots/)
