# Desktop organizer

A local Python organizer with a browser board/table interface. Import TXT or
Markdown, review suggested categories, correct labels, and export category
files. Its workspace is independent of the Android app.

[Project overview](../README.md) · [Architecture](../docs/architecture.md) · [Contributor guide](../CONTRIBUTING.md)

## Quick start

You need Python 3.10 or later. Run these commands from the repository root on
Linux or macOS:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -e ./desktop
export NOTE_PROCESSING_HOME="$PWD/.local/demo"
python -m note_processing download
python -m note_processing serve --root desktop/examples --output .local/demo-export
```

Open [http://127.0.0.1:8765](http://127.0.0.1:8765) in your browser. The server
prints its address and the selected input/output directories. Use the included
`notes.md` example to explore importing and reviewing categories. Stop the
server with **Ctrl+C**.

`NOTE_PROCESSING_HOME` selects a separate demo workspace for the commands in
this shell. It contains the downloaded model, SQLite database, confirmed labels,
and local training/configuration files. It does not alter an existing default
workspace. On Windows, create/activate the equivalent Python virtual environment
and set `NOTE_PROCESSING_HOME` using your shell's environment syntax.

The model command downloads pinned, checksum-verified MiniLM weights and a
tokenizer from Hugging Face. It contacts the model host without uploading notes.
Unlike the Android built-in classifier, the desktop pipeline requires the
encoder download before classifying notes. Once dependencies and model files
are installed, ordinary classification runs locally.

## Try the command line

With that virtual environment and workspace setting still active:

```bash
python -m note_processing classify --text "Remember to book the train."
python -m note_processing organize --file desktop/examples/notes.md --mode lines --output .local/demo-export
```

`classify` prints JSON with the suggested label, scores, and review information.
`organize` writes Markdown category files. Existing files require explicit
overwrite permission; choose a new output directory when trying another run.
For all commands:

```bash
python -m note_processing --help
python -m note_processing serve --help
```

## Learning and data

The pipeline combines intent cues, MiniLM embeddings, a trainable classifier,
and confirmed-example memory. It supports optional neighboring context and
segmentation by line, paragraph, or sentence. Corrections supply labels for
personal head training:

```bash
python -m note_processing train
```

Optional encoder fine-tuning requires the additional training dependencies:

```bash
python -m pip install -e './desktop[training]'
python -m note_processing train --encoder
```

Install the optional dependencies only when you intend to fine-tune; ordinary
head training uses the default install. Training needs sufficient confirmed
examples and applies a held-out adoption check.

The HTTP server binds to `127.0.0.1`. SQLite stores notes and corrections locally;
this workspace does **not** use the Android vault's encryption. Exports are
ordinary readable files. Keep sensitive data in a trusted location, and keep
independent copies of originals.

## Checks and troubleshooting

From the repository root with the virtual environment active:

```bash
python -m unittest discover -s desktop/tests
node --check desktop/src/note_processing/web/app.js
```

Node.js is needed for that JavaScript syntax check, not for the running Python
server. Tests cover import preservation, undo, export protection, and training
boundaries.

| Symptom | Check |
| --- | --- |
| “Model missing” | Run `python -m note_processing download` with the same `NOTE_PROCESSING_HOME` setting |
| Port 8765 is busy | Start with `serve --port 8766` and open the printed URL |
| Old categories or notes appear | Check the selected `NOTE_PROCESSING_HOME`; restart the process after changing it |
| Training requests more labels | Confirm distinct examples in every category before training |

`src/note_processing/` contains the classifier, learning, server, and browser UI.
See [Apache 2.0](LICENSE) and the project's [notices](../NOTICE) for licensing.
