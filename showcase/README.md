# Project showcase

A portable project-site design with a working browser notebook, an
approved-example learning lab, and an interactive engineering walkthrough.
It uses static HTML, CSS, and JavaScript with no frontend dependencies,
accounts, analytics, cloud inference, or paid services.

## Preview

From the repository root, with Node.js 22+ and Python 3:

```bash
node showcase/build.mjs
python3 -m http.server 8770 --bind 127.0.0.1 --directory showcase/dist
```

Open [http://127.0.0.1:8770](http://127.0.0.1:8770). Stop the server with Ctrl+C.
The build also produces `showcase/dist/note-chat-showcase.html`: a self-contained
HTML export with inline styles, model code, seed categories, and screenshots.
Use that export when opening a file directly. `showcase/src/index.html` is the
source page and needs the server; it cannot fetch the category data on its own.
The export requires JavaScript, Web Workers, and Web Crypto. Its own content security
policy disables network connections. The owner reviewed the export through a local file URL in Codex’s in-app
browser. Automated browser checks used the normal localhost site; broader
browser compatibility has not been established.
The build copies the current Android category examples and existing app
screenshots; `dist/` is an ignored output directory. Relative assets work from
a static-hosting subdirectory, including a GitHub Pages project site. Publishing is a separate action. A manual GitHub Pages workflow is prepared in
`.github/workflows/showcase-pages.yml`; see [release steps](../docs/release-next-steps.md).
No live deployment is claimed.

## What works

- Type a note and classify it locally in a Web Worker.
- Review the prediction's reason, confirm it, or choose a different category.
- Browse Inbox, category boards, and To-do; complete and reopen tasks.
- Teach the learning lab an exact note, then see the confirmed label used in
  Inbox. Reset clears the memory.
- Explore four stages of the actual Android architecture, its source, and the
  dated readiness evidence. Open genuine Android screenshots and release links.

The JavaScript classifier adapts the built-in lexical path from Android's
`TextClassifier.kt`: 12 cues, 128 hashed word features, the frozen starter head
trained from category seeds, and normalized approved-example memory. Its
384 MiniLM feature slots remain zero. The browser's JavaScript/regex and numeric
behavior are an adaptation, not a binary-equivalent Android runtime.
During development, its predicted labels matched the compiled Android classifier
on 24 synthetic examples, including every category and several Unicode inputs.
That fixture comparison is not a general accuracy claim.

The demo does not implement MiniLM, personal-head training/held-out adoption,
encryption, Markdown sync, media, or Android services. It labels these boundaries
in the interface rather than fabricating their behavior. All demo records live
in tab memory; there is no localStorage, cookie, database, or input submission.
The browser fetches static files from its host. Normal hosting request logs
are controlled by that host. External links only navigate when the user chooses.

Demo bounds: 2,000 characters per input, 20 displayed notes, 100 remembered
examples, and a bounded feature cache. Unicode handling and input validation
remain subject to browser support. This is a presentation demo, not a place to
store important notes.

## Verify

```bash
node showcase/tests/classifier.test.mjs
node --check showcase/src/app.mjs
node --check showcase/src/classifier.worker.mjs
```

The prototype's design uses the existing teal app identity, warm paper tones,
system fonts, a usable notebook in the hero, and progressively deeper learning
and engineering sections. It respects reduced motion and uses labelled native
controls, keyboard input, and responsive layouts. Real device accessibility
testing remains separate from browser checks.
