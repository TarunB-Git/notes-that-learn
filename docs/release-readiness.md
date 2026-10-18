# Android beta readiness — 18 October 2026

## Current candidate update

Owner reports the phone checklist passed on Samsung / Android 16. The exact
model and tested APK are unspecified; a production-signed upgrade is not yet
confirmed. Later UI additions need a focused retest. Follow
[the ordered release steps](release-next-steps.md).


The working checkout now declares 1.5.1 / code 9, with a separate Google-free
build and a preview that installs as `dev.notechat.preview` beside the published
app. This version has not been tagged, signed with the production key, published,
or accepted by F-Droid.

The two original ZIP backup findings below have code fixes and regression
coverage: export enforces restore's aggregate limits before writing, verifies
attachment lengths, and caps the encrypted output; merge validates the entire
result and rejects cross-type identities before importing media or changing
notes. Oversized Markdown-folder files are also rejected before replacing the
last recoverable folder snapshot.

ONNX Runtime is pinned to 1.31.0. All four packaged ARM64 libraries now have
16 KB ELF LOAD alignment and the APK passes ZIP alignment. MiniLM inference on
a real 16 KB device remains untested. The original observations below are kept
as the audit trail, not as a list of unfixed candidate defects.

Release build tools are pinned to 35.0.0, and checkout-dependent VCS metadata is
excluded from release APKs. F-Droid source/APK scans and metadata lint have been
run locally; full build-server acceptance and signature reproduction still need
the tagged developer-signed release. See [F-Droid preparation](fdroid-release.md)
for final verification results and handoff.

### Controls added after the owner phone report

Both Android builds now share a home-screen Chat/Notes toggle, debounced Notes
editing autosave with pause/navigation flushing, and separate theme/launcher
color presets. Notes edits keep their category but invalidate the old confirmed
training label. Blank edits keep the last nonempty note; failed saves keep the
editor and draft available. Chat editing keeps explicit save/cancel behavior.

Both builds have 127 passing tests, including new autosave, launcher API
30/35, theme backup/defaults and contrast coverage. Lint has no errors. Final
APK hashes are recorded in the distribution verification file. The owner phone report predates these controls.
Launcher aliases preserve the real activity/share target; existing pinned
shortcuts and OEM icon caching need a Samsung check. Theme colors are backed up;
launcher color is device-local. See `docs/android-appearance.png` for an actual
Robolectric rendering of the new Display controls.

## Decision and scope

The next target, confirmed by the owner, is a small Android beta with real users.
Recommendation: fix the two reproduced backup defects, resolve native-library
compatibility, then test a signed candidate on real phones before inviting users
to depend on it. The feature set is sufficient for that beta. This review does
not establish readiness for a broad public launch or certify security.

This was an assessment of the existing checkout, including its pre-existing
uncommitted changes. Application code was not changed by this review. No personal
notes, recordings, or local note database were inspected. Reproductions used
synthetic data and the compiled application classes.

## Observed verification

| Check | Result |
| --- | --- |
| Android unit and Robolectric tests, forced fresh execution | 111 passed; 0 failed, errored, or skipped |
| Desktop unit tests | 7 passed |
| Desktop JavaScript syntax | Passed |
| Android debug APK build | Passed |
| Android minified preview APK build | Passed; this variant uses the debug signing key |
| Android debug lint | 0 errors, 22 warnings |
| Native ELF segment inspection in preview APK | ONNX JNI bridge has 4 KB alignment; other four ARM64 shared libraries have 16 KB alignment |
| APK ZIP alignment check | Passed; this does not establish ELF compatibility |
| Real phone/emulator workflows | Not run; no device was connected |
| Current production-signed build | Not built locally during this review |

The repository has working release infrastructure. GitHub shows successful
[checks](https://github.com/TarunB-Git/note-processing/actions/runs/38259927599)
and a successful
[signed release workflow](https://github.com/TarunB-Git/note-processing/actions/runs/38259927592)
for commit `93b392b8f1682fe0a7df8fa986a55bac26238d63` on 12 October.
[v1.5.0](https://github.com/TarunB-Git/note-processing/releases/tag/v1.5.0)
has published APK, AAB, and checksum assets. Those remote results cover that
commit, not the subsequent local changes. At the initial audit the checkout declared version 1.5.0 / code 8. The current
candidate declares 1.5.1 / code 9 and still needs a reviewed commit containing
all intended files, including currently untracked sources.

Local commands used, from the repository root unless noted:

```bash
.venv/bin/python -m unittest discover -s desktop/tests
node --check desktop/src/note_processing/web/app.js
```

From `android/`, with JDK 17 and the installed Android SDK configured:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
ANDROID_HOME=/home/tar_guest/Android/Sdk \
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assemblePreview --no-daemon --offline

JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
ANDROID_HOME=/home/tar_guest/Android/Sdk \
./gradlew :app:testDebugUnitTest --rerun --no-daemon --offline
```

## Fix before expanding the beta

### 1. Export can produce a backup that restore rejects — fixed in candidate

**Observed:** a valid workspace containing 17 notes, each with 1,000,000 ASCII
characters, passes validation. Its portable JSON is 17,007,089 bytes. Backup
export succeeds and creates a roughly 20 KB compressed, encrypted ZIP. Reading
that exact file with the correct password throws
`IllegalArgumentException: Backup entry is too large`.

**Cause:** `BackupArchive.write` accepts the workspace and serializes it without
enforcing the reader's 16 MiB decompressed workspace limit. Individual notes are
allowed up to 1,000,000 characters. This mismatch can also affect accumulated
ordinary notes; compression does not solve the decompressed size limit.

**Relevant code:** `BackupArchive.kt:29`, `:50`, `:67`, `:104`, `:131`.

**Required outcome:** every successfully exported backup must be readable by the
matching app version. Apply consistent workspace, attachment-count, and total
size limits on both sides, with a clear explanation before export if a limit
prevents backup. Preserve the existing encrypted format where feasible.
Add a regression covering the aggregate workspace boundary, not just one long
note. `MarkdownFolder.kt:23` separately caps each managed text file at 16 MiB;
review that boundary against generated category files too.

### 2. Restoring an older backup after promoting a chat creates invalid state — fixed in candidate

**Observed:** create a private chat and a note, back it up, promote that chat to
a category, then merge the old backup. Both input states pass validation. Merge
succeeds, but the result fails `BackupArchive.validate` because the same ID now
belongs to both a category and a private chat. A subsequent full backup is
therefore blocked by validation.

**Cause:** merge deduplicates categories against current categories and chats
against current chats, without resolving collisions between those two groups.
Chat promotion deliberately preserves the chat's ID as the new category ID.

**Relevant code:** `BackupArchive.kt:37`, `:119–127`; `Domain.kt`,
`ChatEngine.promoteChat`.

**Required outcome:** detect this case before changing the workspace. Preserve
the current promoted category and either explicitly resolve the restored chat
identity and its message references or reject the conflicting restore with an
actionable explanation. Validate the complete merged state before persistence.
Cover backup → promotion → restore and category/chat limits after merging.

### 3. Native ONNX library is not aligned for 16 KB devices — packaging fixed in candidate

**Observed:** `lib/arm64-v8a/libonnxruntime4j_jni.so` in the minified preview APK
has ELF LOAD segment alignment `0x1000` (4 KB). The packaged ONNX runtime core,
Vosk, JNA dispatch, and translation libraries have `0x4000` alignment. The ZIP
alignment check passes, demonstrating why both checks are necessary.

**Relevant dependency:** `com.microsoft.onnxruntime:onnxruntime-android:1.20.0`
in `android/app/build.gradle.kts`.

**Required outcome:** select a compatible runtime or rebuild the bridge, inspect
all packaged native libraries, and run optional MiniLM inference on a 16 KB
device/emulator. ELF inspection establishes a compatibility defect; this review
did not reproduce a crash on hardware. See the official
[16 KB compatibility guidance](https://developer.android.com/guide/practices/page-sizes).

## Evidence still needed for the beta

- Use a production-signed candidate and verify an upgrade from published v1.5.0
  preserves notes, media, approvals, and settings. The preview signing key is
  different from the production key and cannot establish this upgrade path.
- On at least two representative ARM64 phones, exercise fresh installation,
  sending/editing notes, import, recording/playback, app lock, background/reopen,
  widgets, reminders, folder sync, and encrypted backup/restore into an isolated
  installation. Keep the original data and backup intact during recovery tests.
- Test permission denial, offline operation, interrupted writes or process
  termination, and loss of folder access. Verify visible recovery and that a
  failed operation does not silently discard notes.
- Exercise a larger synthetic workspace, for example 1,000 and 10,000 notes.
  Record startup, send/search latency, and memory on the lower-memory phone.
  Storage currently serializes the full workspace, so scale needs measurement.
- Check TalkBack, large font settings, keyboard handling, and both themes on
  real screens. JVM UI tests are valuable but do not establish these outcomes.
- Keep a brief result record with candidate version, device/Android version,
  scenario, observed result, and any blocker. Add an emulator smoke check to CI:
  `RuntimeChecks` exists but `.github/workflows/checks.yml` currently runs JVM
  tests, lint, and debug assembly only.

## Small additions with a concrete benefit

Documentation follow-up on 18 October: the repository now includes a first-use
guide, an engineering case study, build instructions, and a public bug-report
form. These presentation changes are local until committed and published. They
address repository onboarding and feedback; the application still needs its own
support entry point and a complete privacy policy for a Play release. The beta
blocking findings and device-verification limits above remain open.

1. **Recovery confidence:** show the last successful backup and distinguish
   creating a file from verifying that it restores. Add an explicit recovery
   guide, including forgotten backup passwords and unavailable device keys.
2. **Private bug reporting:** an opt-in diagnostic export with app/device version
   and actionable errors, excluding notes, attachments, category examples, and
   weights. Keep it local until the user chooses to share it. Existing classifier
   diagnostics and the model report are useful foundations, but are not general
   app failure reporting.
3. **About, privacy, and support:** give testers one place to find version,
   known limits, third-party data handling, recovery instructions, and a feedback
   route. No dedicated privacy policy or general support flow was found in the
   inspected repository/app text.
4. **Measure classification with beta notes:** keep a separate set of fresh,
   locally labelled examples and inspect mistakes by category. Decide about
   model replacement after this evidence; the existing feature set and optional
   MiniLM are sufficient to begin that experiment.

These are recommendations, not approved changes to the application scope.

## Later public Google Play release

The app currently targets API 35. Google's current policy requires API 36 or
higher for new mobile app submissions and updates from 31 August 2026, with
extensions described in Play Console. Plan a target/compile SDK and compatible
build-tool upgrade with behavior testing before a Play submission; an APK beta
distributed directly does not itself require a Play submission.
[Target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)

Prepare an accessible privacy policy, accurate Data safety disclosures,
store listing, support details, and the account-specific testing steps in Play
Console. Optional Google ML Kit behavior must be included in the disclosures.
[Google Play user data policy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en-GB)

## Owner's useful next actions

- Recruit 3–5 intended users and ask them to use the app for one or two weeks
  after the backup and compatibility fixes and the signed-device checks.
- Supply the intended phones for the device checks, or run the supplied scenarios
  on them. Include the candidate version and Android version in feedback.
- Keep release signing key recovery and backup-password recovery arrangements
  under your control. Never put keys or passwords in issues or diagnostic files.
- Define a successful beta in observable terms: users can capture and find notes,
  correct suggestions, upgrade, and recover their data without assistance.

The original recommendation was to repair the backup paths and native bridge
before a signed phone candidate. Those code fixes are implemented. The next
action is the focused controls retest and production-signed upgrade check in
[the release handoff](release-next-steps.md).

## Current verification follow-up — 18 October 2026

Both distributions pass 127 JVM/Robolectric tests with no failures, errors or
skips, and lint reports no errors. This count includes the five ZIP backup
regressions and the folder-size recovery regression added after the original
111-test audit, plus UI autosave, task editing, launcher and palette coverage.
The final palette-only adjustment has a focused AppearanceTest rerun on both
builds, including text/accent/button contrast in every palette and mode. Native
ELF and ZIP alignment now pass. The candidate retains
version-1 backup compatibility within the existing reader limits.

The [F-Droid release plan](fdroid-release.md) records scanner, clean-build,
signing and phone handoff details. No production-signed phone test, public
v1.5.1 tag, or F-Droid submission is claimed. Local artifact verification is in
[the candidate verification record](../distribution/fdroid/verification.txt).
