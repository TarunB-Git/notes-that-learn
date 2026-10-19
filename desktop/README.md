# Desktop organizer

A local Python organizer with a browser board/table view, category correction,
and Markdown export. Its SQLite workspace is separate from the Android app.

## Run

Python 3.10+, from the repository root on Linux/macOS:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -e ./desktop
export NOTE_PROCESSING_HOME="$PWD/.local/demo"
python -m note_processing download
python -m note_processing serve --root desktop/examples --output .local/demo-export
```

Open [localhost:8765](http://127.0.0.1:8765); stop with Ctrl+C.
Windows uses the equivalent virtual-environment and environment-variable syntax.
The download fetches pinned, checksum-verified MiniLM files from Hugging Face.
Notes stay local. The selected workspace holds its model, notes, and labels;
SQLite and exported files do not use Android vault encryption.

## CLI and learning

```bash
python -m note_processing classify --text "Book the train."
python -m note_processing organize --file desktop/examples/notes.md --mode lines --output .local/example-export
python -m note_processing train
python -m note_processing --help
```

Classification combines MiniLM, intent cues, approved-example memory, and a
trainable head. Confirmed labels enable personal training with a held-out
adoption check. Optional encoder fine-tuning uses:

```bash
python -m pip install -e './desktop[training]'
python -m note_processing train --encoder
```

## Verify

```bash
python -m unittest discover -s desktop/tests
node --check desktop/src/note_processing/web/app.js
```

“Model missing” means download using the same `NOTE_PROCESSING_HOME`.
Use `serve --port 8766` if 8765 is occupied.

[App design](../README.md#engineering-worth-exploring) · [Contributing](../CONTRIBUTING.md) ·
[License](LICENSE) · [Notices](../NOTICE)
