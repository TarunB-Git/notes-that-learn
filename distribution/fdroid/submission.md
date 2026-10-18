# F-Droid inclusion request draft

Do not submit until the v1.5.1 tag and production-signed free APK are public,
phone checks pass, and the Disabled marker in the recipe can be removed.

Name: Note Chat
Application ID: dev.notechat
License: Apache-2.0
Source: https://github.com/TarunB-Git/note-processing
Issue tracker: https://github.com/TarunB-Git/note-processing/issues
Candidate: 1.5.1 / versionCode 9 / ARM64 / Android 11+

Note Chat captures notes in a chat inbox or category boards. It suggests
categories locally and learns from labels the user confirms. It includes
private notebooks, recordings, attachments, reminders, a task widget, Markdown
folder export and encrypted backups. Optional speech and MiniLM models download
on request and then run locally. No account or classification server is needed.

The F-Droid build excludes all Google translation/OCR SDKs. Build with
`-Pfdroid=true`, removing `android/google` and the unrelated historical
`archive/v1` before scanning/building. The recipe is in
`distribution/fdroid/dev.notechat.yml`; localized listing assets are in
`fastlane/metadata/android/en-US`.

Optional WhatsApp forwarding and Google Drive backup destinations are disclosed
under NonFreeNet. They are user-selected integrations, not bundled proprietary
SDKs or required services. Core notes and classification work offline.

The production certificate fingerprint is recorded in
`distribution/fdroid/production-cert-sha256.txt`, verified against the public
v1.5.0 APK. The release workflow builds/signs a separate free APK with the same
key. The recipe points to that free artifact for signature reproduction.

Attach final source/APK scan results, metadata lint, clean-build comparison,
and phone results from docs/fdroid-release.md. Do not present a local build as
F-Droid acceptance or as a verified production-signed device upgrade.

Preparation update (replace with the public release's final evidence): both
Android distributions passed 127 JVM/Robolectric tests and lint with no errors.
Coverage includes Notes autosave/failure/retry, To-do text editing, theme backup
compatibility and palette contrast, and launcher switching on API 30/35.
The owner reports earlier phone checks passed on Samsung / Android 16. Exact
model, tested APK and production-signed upgrade still need recording, and the
new controls require a focused retest. Do not describe these as F-Droid server
acceptance or real 16 KiB-device inference results.
