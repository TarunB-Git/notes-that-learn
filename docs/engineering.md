# Engineering case study: a notebook that learns from confirmed labels

[Try the app](getting-started.md) · [Architecture reference](architecture.md) · [Build and checks](../CONTRIBUTING.md)

## The problem and the main interaction

Notes sent to yourself are quick to capture, but thoughts, ideas, questions, and
tasks collect in one stream. Note Chat keeps that familiar capture interaction
and adds suggested categories, review controls, and alternative views of the
same records.

A useful example is “Book the train for Friday.” It enters through the composer
or Android sharing, receives a suggested category, and appears in the To-do
view if assigned there. Confirming or correcting the label supplies a learning
example. Completing the task updates the original record wherever it appears.

The implementation spans a native Android app, a Python desktop organizer, and
an archived SVM experiment. Android and desktop are independent products with
related classification ideas; there is no cross-device sync service.

## Decisions and tradeoffs visible in the implementation

### Keep capture independent of a model download

The Android classifier can use wording cues, 128 hashed word features, and a
small softmax classifier without downloading a transformer. Optional MiniLM
adds 384 embedding dimensions; 12 intent/formatting cues complete the
524-dimensional input. A softmax layer converts the weighted features into
category scores.

This provides a small local starting point and permits learning on the phone.
The tradeoff is that lexical cues can miss indirect intentions, while general
sentence similarity can confuse topic with intent. Adding an encoder therefore
needs evaluation on actual note categories.

**Read:** [TextClassifier.kt](../android/app/src/main/java/dev/notechat/TextClassifier.kt),
[MiniLm.kt](../android/app/src/main/java/dev/notechat/MiniLm.kt),
[classifier tests](../android/app/src/test/java/dev/notechat/ClassifierTest.kt).

### Make user approval the training boundary

Exact matches to approved examples take precedence. With MiniLM enabled, close
semantic matches can reuse approved labels. A prediction or rejection does not
become a training label. Editing, splitting, merging, or deleting approved text
revokes the affected example.

Personal head training requires six distinct approvals per category. Stable
assignments hold out roughly 20% of examples, excluding their text from fitting,
starter seeds, and correction memory during evaluation. The candidate is
adopted when its held-out macro F1 meets the starter baseline. Macro F1 gives
each category equal weight, which helps expose failures in smaller categories.

This guards against teaching predictions back to the model or testing on
remembered answers. A small hold-out can still be noisy, and repeated use of
the same hold-out does not replace evaluation on fresh notes. Android retrains
the small classifier layer; the transformer stays frozen. Desktop has an
optional SetFit encoder-training path.

**Read:** [message and approval operations](../android/app/src/main/java/dev/notechat/Domain.kt),
[Android training and evaluation](../android/app/src/main/java/dev/notechat/TextClassifier.kt),
[desktop learning](../desktop/src/note_processing/learning.py).

### Give multiple views one source of truth

Inbox, categories, private chats, stars, and tasks filter the same message
collection. A change to a message therefore affects every view. Native Views
and RecyclerView supply the interface; message operations live in `ChatEngine`,
serialization in `StateJson`, and encrypted persistence in `Vault`.

The screen coordinator remains substantial. Further growth would benefit from
extracting focused screen and lifecycle responsibilities while retaining the
tested domain operations. The controls in `ui/chat/` share the application's
package and types; they are reusable source components, not a published UI SDK.

**Read:** [Domain.kt](../android/app/src/main/java/dev/notechat/Domain.kt),
[message rows](../android/app/src/main/java/dev/notechat/ui/chat/MessageRows.kt),
[UI regression tests](../android/app/src/test/java/dev/notechat/ChatUiTest.kt).

### Support editable files without losing message identity

Android document-tree access grants a chosen folder. Markdown files retain
stable message markers and block IDs, while a baseline supports three-way
reconciliation of app and external edits. Conflicts preserve an external copy.
Pending identities and file versions support retries after interrupted writes.

This lets ordinary editors participate in the workflow. It also requires
explicit behavior for moved or missing blocks, conflicting edits, corrupted
markers, and lost access. The files are readable exports outside the vault's
protection. Synchronization waits for idle/background time to avoid doing
folder work on every tap.

**Read:** [Markdown reconciliation](../android/app/src/main/java/dev/notechat/MarkdownWorkspace.kt),
[document-tree adapter](../android/app/src/main/java/dev/notechat/MarkdownFolder.kt),
[folder integration tests](../android/app/src/test/java/dev/notechat/MarkdownFolderTest.kt).

### Treat protection and recovery as separate responsibilities

App snapshots and attachments use AES-GCM encryption. Android Keystore protects
the data key, and optional app lock adds device authentication. Portable ZIP
backups use a password-derived key and authenticated encrypted entries. A
separate opt-in widget projection controls task visibility while locked.

Local encryption does not protect readable exports or copies shared with
another app. Recovery also depends on correctly matching writer and reader
limits. The readiness review reproduced backup failures despite passing tests. The
current candidate adds aggregate limits and merge validation with regressions;
real-device recovery checks remain necessary.

**Read:** [Vault.kt](../android/app/src/main/java/dev/notechat/Vault.kt),
[BackupArchive.kt](../android/app/src/main/java/dev/notechat/BackupArchive.kt),
[backup tests](../android/app/src/test/java/dev/notechat/BackupArchiveTest.kt).

## What is measured, and what remains open

The [18 October 2026 review](release-readiness.md) records 111 passing Android
tests, 7 passing desktop tests, successful debug and minified preview builds,
and zero lint errors. Those are dated checkout results. The
[CI workflow](../.github/workflows/checks.yml) provides checks for subsequent
commits; the [release workflow](../.github/workflows/release.yml) builds signed
artifacts, verifies signatures and versions, and creates a draft release.

Tests cover domain policies, imports, approval boundaries, Markdown recovery,
and native UI behavior under Robolectric. The instrumented
[RuntimeChecks](../android/app/src/androidTest/java/dev/notechat/RuntimeChecks.kt)
exercise device storage and a launch/share smoke path, but are not currently
part of CI. Real device checks remain necessary for biometrics, media, widgets,
accessibility, and upgrade/recovery behavior.

Classifier diagnostics expose session median/p95 classification latency and
whole-app memory. These are not end-to-end UI benchmarks. No general accuracy,
retention, or user-adoption claim is established for the current app. The
archived three-class SVM report describes its original dataset, rather than
current Android performance.

## Next evidence to collect

For the planned small beta, the next useful evidence is successful recovery
with the backup fixes in the candidate, a signed upgrade from v1.5.0, inference on a
16 KB device, and intended users completing capture/review/retrieval without
assistance. Fresh labelled notes can then show whether the classifier needs
better examples, clearer category boundaries, or a model change.

The [readiness report](release-readiness.md) contains the reproduced failures and
acceptance checks. The [contributor guide](../CONTRIBUTING.md) contains the
commands needed to work on them.

## A 45-second demo to record

Use the synthetic examples from the [first-use guide](getting-started.md). Frame
the recording around one successful workflow and show the installed version in
the caption.

| Time | Show | Explain |
| --- | --- | --- |
| 0–10 seconds | Send a thought and a task in Inbox | Capture notes with the speed of sending yourself a message. |
| 10–20 seconds | Confirm a category or choose a different one | Your confirmed labels supply learning examples. |
| 20–30 seconds | Find the task in To-do and complete it | The task list and Inbox use the same records. |
| 30–45 seconds | Switch to Notes layout and search for a sample note | Browse by category or retrieve the original text. |

A real recording can demonstrate the interaction that screenshots cannot.
Add it to the README once recorded and reviewed. Until then, the README uses
existing app screenshots and links directly to the APK and guided walkthrough.
