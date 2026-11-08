# Start using Note Chat

Download an APK from [v1.5.1](https://github.com/TarunB-Git/note-processing/releases/tag/v1.5.1).
It requires Android 11+ and ARM64. Choose the standard APK for optional Google
translation/OCR, or the `fdroid-arm64.apk` build without those tools. The AAB is
for store distribution, not direct installation.

Open the APK on your phone and allow installation from your browser or file
manager if Android asks. No account or model download is needed to begin.

## Capture, organize, retrieve

1. Send a note in Inbox: “Book the train for Friday.”
2. Confirm its category with thumbs-up, or tap the category to change it.
3. Open To-do to complete the task, or find it with search.
4. Use the turning arrows beside Settings to switch between Chat and Notes.
   Notes edits autosave after a pause or when leaving the editor; the checkmark
   finishes editing. Chat edits use Save/Cancel.

A blank Notes edit preserves the previous text. Delete removes the note.
Settings → User guide walks through the actual controls.

## Make it yours

| Setting | Purpose |
| --- | --- |
| Display | Layout, Light/Dark/System, theme palette, launcher icon color, task widget |
| Storage & backups → Categories | Category names, descriptions, and examples |
| Classification & learning | Optional MiniLM and personal classifier training |
| MODs | Optional speech, reminders, and standard-build translation/OCR |
| Privacy & security | App lock using your device authentication |

Only confirmations and manual category choices supply training labels.
Thumbs-down requests another suggestion. Unsorted includes unapproved notes;
private chats stay outside classification. Editing approved text revokes its
old approval. Personal training unlocks after six distinct approvals per category.

## Data and recovery

App notes and attachments are encrypted locally. **Settings → Storage & backups**
contains encrypted ZIP backup/restore and a Permanent Markdown folder. Markdown
and shared exports are readable copies outside app-lock protection. Folder
reconciliation runs after idle time, when leaving the app, or through Sync now.
Each category or private chat has one Markdown file. Dates and sync markers are
hidden comments; edit the text or append new paragraphs without changing those
markers. New and edited category notes are sorted on the next sync and need your
confirmation before they teach the classifier. Private chats stay private. The
folder still retains workspace metadata and attachments for recovery.

**Settings → Display → Move forwarded messages** moves a note between chats
instead of keeping a second copy. Inbox remains an overview of category notes.

Drive can hold a chosen backup; it does not create automatic device sync.

A backup needs its password to restore. App lock cannot unlock a forgotten
backup password. Deleting the app removes its private storage, so recovery needs
an exported backup or an intact connected Markdown folder. Deleted notes remain
in Recently deleted for 90 days; completed tasks archive after 24 hours.

The built-in classifier runs locally. Optional MiniLM and Vosk models download
on request; Vosk transcribes English locally. In the standard build, ML Kit
processes translation/OCR locally and sends Google SDK usage/performance metrics;
OCR and language identification require Google Play services. Reminders use
inexact alarms and can be delayed by battery saving.

[Report a problem](https://github.com/TarunB-Git/note-processing/issues/new?template=bug_report.yml)
with app/Android versions and a small invented example.
[Build guide](../CONTRIBUTING.md)

## Lists, tables, and recordings

In a category or private chat, open its menu and choose **Layout**: Notes/chat,
Checklist, or Table. You can also choose a layout when creating it. Switching
layouts keeps the notes. Custom checklists keep checked items visible; the built-in
To-do category continues to archive completed tasks after 24 hours.

Tables have editable column names and cells. Tap a heading to rename it, or **+**
to add a column. Send a message with the normal composer to create a row, tap
a cell to edit its fields, and hold a row for message actions. Small tables fit
the screen; wider ones scroll sideways with aligned headers. There are no formulas.
Table cell values and layout settings remain in backups and the Markdown folder.

Tap **Transcribe recording** below an audio message. Transcription stays on the
phone and can continue while you use another app. Its progress notification has
a Cancel action. When finished, open **Show transcript** below the recording.
The saved transcript is reused, including after reopening the app; failures can
be retried. If Android stops the process before completion, retry from the recording.
