# Browser showcase

A static product page with a working note inbox, category boards, and a
confirmed-example learning demo. HTML, CSS, and JavaScript; no frontend
packages, account, analytics, or inference API.

## Run locally

From the repository root, with Node.js 22+ and Python 3:

```bash
node showcase/tests/classifier.test.mjs
node showcase/build.mjs
python3 -m http.server 8770 --bind 127.0.0.1 --directory showcase/dist
```

Open [localhost:8770](http://127.0.0.1:8770); stop with Ctrl+C.
`showcase/dist/index.html` is the hosted entry point.
`showcase/dist/note-chat-showcase.html` is a self-contained file export.

The demo adapts Android's lexical classifier and approved-example memory.
MiniLM, personal-head training, encryption, and native services belong to the
Android app. Demo notes live in tab memory and disappear on reload.

The [Pages workflow](../.github/workflows/showcase-pages.yml) builds and deploys
the site on relevant pushes to `main`. Build outputs stay out of Git.
