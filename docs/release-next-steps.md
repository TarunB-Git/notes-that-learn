# Ship 1.5.1: phone → GitHub → website → F-Droid

The source candidate is **1.5.1 / versionCode 9**. Nothing in this checkout has
been pushed, tagged, published, or submitted to F-Droid by this preparation.
Your Samsung / Android 16 phone checks passed according to your report. The
exact Samsung model, APK tested, and production-signed upgrade result have not
been recorded. The controls added afterwards need the short retest below.

## 1. Retest the changed controls

Use the refreshed `dist/fdroid-candidate/note-chat-v1.5.1-fdroid-preview.apk`.
It installs beside the real app as **Note Chat Preview**. Do not delete the real
app or its data. The common UI code is used by both Android distributions.

- On the home screen, use the turning arrows beside Settings. Switch Notes →
  Chat → Notes. Confirm both layouts show the same notes, search still works,
  and an unfinished message is still there when you return to its notebook.
- In Notes mode, open a category and tap a note. Type, pause for a second, then
  press Back. Reopen it and confirm the text saved without pressing the checkmark.
  Type again, immediately go Home on Android, then reopen. Check again. The
  checkmark finishes editing; Back also keeps the saved changes in Notes mode.
- Confirm the cursor does not jump while you type. A blank edit keeps the last
  nonempty note; use Delete to remove a note. Chat mode keeps its explicit
  save/cancel behavior. Editing text removes the previous confirmed training
  label; the Notes category stays in place until you change/confirm it.
- In Settings → Display, try each Theme colors swatch, Light, Dark and System.
  Check Chat and Notes, text legibility, keyboard, dialogs, and large text.
  Reopen the app and check the theme remains selected. Android's animation
  setting controls the brief fade when the palette changes.
- Try App icon color, return to Samsung's launcher and open the app from the
  changed icon. Check there is one app entry, then switch back to Classic.
  Android 11/12 and 13+ use different launcher update APIs; automated tests cover
  both. Samsung may refresh cached icons slowly. An old pinned shortcut may
  need to be replaced after the launcher-entry migration. The app and its
  share target remain enabled. Icon choice is local to this installation;
  theme colors travel with the workspace backup.

Record the exact phone model, Android 16, APK filename, and any failure. These
changes do not require repeating every earlier check, but production-signed
upgrade and backup recovery must be covered before a public release.

## 2. Verify the release signature and upgrade

Keep an independent encrypted backup of the real app and its password. Test
that it restores into the isolated Preview app before relying on it.

From the project root, sign the free unsigned APK locally:

```bash
export ANDROID_HOME=/home/tar_guest/Android/Sdk
bash distribution/fdroid/sign-release.sh \
  dist/fdroid-candidate/note-chat-v1.5.1-fdroid-unsigned.apk \
  dist/fdroid-candidate/note-chat-v1.5.1-fdroid-signed.apk
```

The helper prompts for the existing keystore path/alias and passwords locally.
It rejects a signing certificate that differs from published v1.5.0. Keep the
private keystore/passwords out of Git, issues, chat, and APK attachments.

Install that signed candidate over v1.5.0, without uninstalling. If you already
upgraded to an earlier local 1.5.1 candidate, update that installation and keep
your original v1.5.0 upgrade result; do not downgrade or delete its data.
Confirm notes, attachments, labels, app lock, settings, widget and reminders survive. Standard
and free release builds both use `dev.notechat`, so they replace one another;
the separate Preview app cannot prove an upgrade works. The free build omits
Google translation/OCR. It retains the other features.

## 3. Commit the source and push it

Include the app changes already in the working tree, their regression tests,
F-Droid build separation, listing images/text, release scripts, website source,
and documentation. Review your diff first: there were existing uncommitted app
changes before this preparation, and they must be included intentionally.

From the project root:

```bash
git status --short
git diff --stat
git add .github NOTICE README.md CONTRIBUTING.md android desktop/README.md docs distribution fastlane showcase
git diff --cached --name-status
git diff --cached --check
git commit -m "Prepare Note Chat 1.5.1 and F-Droid distribution"
git push origin main
```

Do not force-add ignored files. `dist/`, `showcase/dist/`, Android build/cache
folders, `local.properties`, APK/AAB files and keystores stay out of Git. APKs
belong in release assets, not the source tree. Keep the Gradle wrapper tracked.
The deletion of the old common `Mods.kt` is intentional: its Google version is
now under `android/google`, and the free implementation is under `android/fdroid`.

Open the repository's **Actions → Checks**. Wait for desktop/browser checks and
both Android jobs to pass. Local checks do not prove the remote workflow passed.
Fix failures before tagging; do not tag a different commit from the one tested.

## 4. Create the GitHub release

The existing Signed release workflow expects these repository Actions secrets:
`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
`ANDROID_KEY_PASSWORD`. Keep working existing secrets; do not rotate the key.
If they are missing, configure them privately in GitHub Settings → Secrets and
variables → Actions, or build/sign locally instead. Never commit the key.

After source CI and the phone retest pass:

```bash
git tag -a v1.5.1 -m "Note Chat 1.5.1"
git push origin v1.5.1
```

The workflow creates a **draft** release with:

- `note-chat-v1.5.1-arm64.apk`: standard build, including Google tools.
- `note-chat-v1.5.1-fdroid-arm64.apk`: Google-free build signed with the same key.
- `note-chat-v1.5.1.aab` and `SHA256SUMS.txt`.

Download the actual free APK from that draft and check it on the phone before
publishing, so you test what users will receive. Review the release notes:
mode toggle, Notes autosave, themes/icon colors, recovery fixes, Google-free
build, Android 11+ / ARM64. Publish the draft when satisfied. Do not call it an
accepted F-Droid release yet. Do not move or reuse v1.5.0. Once v1.5.1 is public,
use a new version/code for subsequent binary changes.

## 5. Publish the website for free

The showcase needs only static hosting. The prepared
`.github/workflows/showcase-pages.yml` is **manual**, so pushing source alone
will not publish it.

1. In GitHub repository Settings → Pages, choose **GitHub Actions** as the source.
2. After the app release is public, update the two GitHub release links in
   `showcase/src/index.html` from `v1.5.0` to `v1.5.1`, and the visible version
   caption. Update the README and first-use guide's download links too, while
   retaining historical v1.5.0 audit references. Keep the F-Droid button's
   preparation status until a listing exists.
3. Commit/push the website update. Open Actions → **Publish showcase** →
   **Run workflow**, selecting `main`.
4. Use the URL shown by the successful deployment. The expected project URL is
   `https://tarunb-git.github.io/note-processing/`; it is not a verified live URL yet.
5. On phone and desktop, check the demo, label correction, GitHub download,
   F-Droid status, screenshots and Report a problem link. Add the live URL to
   the repository About website field, your CV/portfolio and the README.

For later site changes, push the sources and run Publish showcase again.
Do not upload the `src/index.html` alone: run `node showcase/build.mjs` and host
all of `showcase/dist/`. The workflow does this. The standalone
`showcase/dist/note-chat-showcase.html` is a separate portable file for sharing.
GitHub Pages hosting and Actions for a public repository are free within
GitHub's service limits; there is no paid domain or backend to configure.

[Official GitHub Pages workflow guide](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages)

## 6. Submit to F-Droid

F-Droid reviews and builds source; uploading an APK alone does not create a
listing. The package already includes a build recipe, public signing certificate
fingerprint, localized description, icon, screenshots and changelog.

1. Ensure `v1.5.1` and the signed **free** GitHub APK are publicly accessible.
   Its download filename must match `Binaries` in
   `distribution/fdroid/dev.notechat.yml`. Keep the standard Google APK separate.
2. Fork `fdroid/fdroiddata` on GitLab and create a branch such as
   `new-app-note-chat` in that fork. Copy the recipe to
   `metadata/dev.notechat.yml` in the fork. Remove its `Disabled` field when the
   prerequisites pass, and replace the candidate-only MaintainerNotes paragraph
   with the actual public tag, artifact and phone results. Add the live website
   using the metadata's `WebSite` field after it is deployed.
3. Include the prepared local verification record with the request. If you have
   F-Droid's build environment, rerun it against the public tag and signed APK;
   otherwise state that build-server verification is pending and let the merge
   request's CI/maintainers perform it. Keep `Binaries` and
   `AllowedAPKSigningKeys` so the exact signed reference APK is verified. Local
   unsigned byte equality is useful evidence; F-Droid must independently
   reproduce the release and its signature.
4. Open one **New App: dev.notechat** merge request to fdroiddata. Use
   `distribution/fdroid/submission.md` as the description, updated with actual
   public release and test results. The root `fastlane/metadata/android/en-US`
   assets are read from the tagged app source; they do not need a duplicate
   listing system. If packaging assistance is needed, use F-Droid's request for
   packaging route instead of opening duplicate requests.
5. Reply to maintainer questions and fix reproducibility or policy issues they
   identify. Acceptance and indexing time are controlled by F-Droid; no fixed
   publication date is promised. Never give maintainers your private signing key.
6. When `https://f-droid.org/packages/dev.notechat/` actually exists, point both
   site F-Droid buttons there, remove the preparation notice, and redeploy.
   Keep GitHub as the immediate APK option.

The recipe removes the Google-only source directory and the historical archive.
Optional Drive backup destinations and WhatsApp forwarding remain disclosed;
they are not required for offline notes. The same signing identity is intended
for compatible upgrades across GitHub and F-Droid through reproducible builds.
Future versions need a fresh versionCode, public tag, signed free APK and updated
recipe/changelog; automatic F-Droid update checks are currently disabled.

[Official submission guide](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/) ·
[Upstream signatures and reproducible builds](https://f-droid.org/en/docs/Reproducible_Builds/)
