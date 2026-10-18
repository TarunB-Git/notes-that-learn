# Your first five minutes with Note Chat

[Project overview](../README.md) · [Download v1.5.0](https://github.com/TarunB-Git/note-processing/releases/tag/v1.5.0) · [Known release gaps](release-readiness.md)

## Install the Android app

You need an ARM64 Android phone running Android 11 or later. On the
[v1.5.0 release page](https://github.com/TarunB-Git/note-processing/releases/tag/v1.5.0),
download `note-chat-v1.5.0-arm64.apk`. The `.aab` is a store-upload artifact,
rather than a file to open on your phone.

Open the APK and, if Android prompts you, allow installation from the browser or
file app you used. You can revoke that permission afterward. No Note Chat
account or classifier download is needed to try the main workflow.

The project is preparing a small beta. Begin with sample notes: the
[readiness review](release-readiness.md) documents unresolved backup and device
compatibility issues. This guide describes the current source; labels or
behavior can differ in an older installed build.

The mode shortcut, Notes autosave and color controls below are new in the
1.5.1 candidate. Published v1.5.0 uses Settings to change layouts and saves
edits explicitly.

## Try capture, review, and retrieval

Open Inbox and send these one at a time:

```text
The city feels quieter after the rain.
What if the library hosted a seed exchange?
Book the train for Friday.
Why do leaves change color?
```

1. Look at the category under each note. These are suggestions for you to review.
2. Use thumbs-up when a suggestion matches what you meant. Tap the category to
   choose a different one; a manual choice also confirms the label.
3. Open the To-do category and complete the train task using its checkbox.
4. Use search to find “seed exchange”.
5. Use the turning-arrow button beside Settings on the home screen to switch
   between Chat and Notes. **Settings → Display → Layout** also changes it.
6. In Notes mode, tap a note to edit. Changes autosave after a short pause and
   when you leave. The checkmark finishes editing. An empty edit keeps the
   previous note; use Delete to remove it. Chat edits still use save/cancel.
7. In **Settings → Display**, choose Theme colors for the app and App icon color
   for the launcher. Light/Dark/System is under Appearance. A brief fade respects
   Android's animation setting; launcher updates can take a moment.

You have now exercised the main workflow: capture quickly, review the
organization, and retrieve the same note from another view. These example
sentences demonstrate the controls; they are not an accuracy benchmark.

<img src="android-todo.png" width="260" alt="To-do screen showing a saved task with a completion checkbox" />

For a walkthrough inside the app, open **Settings → User guide**. It highlights
real controls while you navigate.

## Understand suggestions and private chats

| Control or view | What it means |
| --- | --- |
| Thumbs-up | Confirms the label and makes eligible text available for learning |
| Manual category choice | Confirms your chosen label |
| Thumbs-down | Tries another suggestion without teaching that rejected label |
| Unsorted | Notes still awaiting approval, including notes with suggested labels |
| Chats tab | Private notebooks excluded from classification and, by default, Inbox |
| Train & compare | Fits a personal classifier after six distinct approvals per category and checks held-out results |

Predictions alone do not become training labels. Editing approved text revokes
its approval. Images and recordings need reviewed OCR text or a transcript
before their text can contribute to learning.

To add a category, open **Settings → Storage & backups → Categories** and give
it a description and examples. If two categories could apply to the same note,
decide which meaning you want to use consistently when labelling.

## Add optional tools when you need them

- **Sentence similarity:** Settings → Classification & learning → MiniLM
  sentence encoder. The built-in classifier works without it.
- **Speech, translation, OCR, reminders:** Settings → MODs. Check each tool's
  download and consent information before enabling it.
- **App lock:** Settings → Privacy & security. Set up an Android fingerprint or
  device screen lock first.
- **Home-screen checklist:** Settings → Display → To-do widget. Showing task
  text while the app is locked is a separate opt-in preference.

Reminders use inexact Android alarms and may be delayed by battery saving.
Automatic date/time detection supports English expressions; a picker handles
other dates and times. Vosk speech recognition is English. Translation and OCR
use Google ML Kit; OCR and language identification require Google Play services.

## Know where your data goes

| Location | Contents and protection |
| --- | --- |
| Android app storage | Encrypted notes and attachments, with a data key protected by Android Keystore |
| Permanent Markdown folder | Readable notes, attachments, and workspace metadata in the folder you choose; outside app-lock protection |
| Encrypted backup ZIP | Password-protected portable data; keep its password separately |
| A chosen sharing or Drive destination | A copy you explicitly send or save; the receiving app controls that copy |
| Desktop organizer | Its own local SQLite workspace, separate from Android; it does not use the Android vault encryption |

Classification does not send notes to a server. Optional speech transcription
runs locally. Optional ML Kit translation and OCR process input locally, while
Google receives SDK performance and usage metrics. Downloading models contacts
their hosting services.

Deleting an app note does not erase copies you already exported or shared.
Deleted notes remain recoverable in **Settings → Archives & deletion → Recently
deleted** for 90 days. Completed tasks archive after 24 hours.

## Export and recovery

**Settings → Storage & backups** contains TXT/Markdown import, ZIP import/restore,
the Permanent Markdown folder, and encrypted backup creation. Keep an independent
copy of anything important. Until the known backup defects are fixed, a
successful export alone does not establish that a ZIP will restore; check the
[specific cases and next fixes](release-readiness.md#fix-before-expanding-the-beta).

Folder synchronization waits for 30 seconds without interaction or runs when
you leave the app; **Sync now** is available from the folder controls. The folder
can retain notes after uninstall. Reconnecting it is a recovery path, provided
the folder and access remain available. Keep its readable contents in a trusted
location. Drive backup selection does not create continuous device sync.

## Get help or report a problem

Check the [known release gaps](release-readiness.md) first, then
[report a bug](https://github.com/TarunB-Git/note-processing/issues/new?template=bug_report.yml).
Include your installed app version, Android version, the steps, and what you
expected. Replace personal content with a short invented example. For a visual
issue, use a screenshot only after removing private information.

Developers can follow the [build guide](../CONTRIBUTING.md). For the Python
organizer, use the separate [desktop quick start](../desktop/README.md).
