# Sarathi RAG builder (Python)

Offline corpus builder for **Bhagavad Gita** (Annie Besant / Wikisource primary) and **Mahabharata** (K.M. Ganguli via AASI `txt/` mirror).

## Windows setup

```powershell
cd D:\MyProjects\Sarathi
py -m venv .venv
.\.venv\Scripts\activate
pip install -r tools\rag-builder\requirements.txt
```

## Build (download + normalize + SQLite)

```powershell
python tools\rag-builder\scripts\build_all.py --download --normalize --index
```

## Manual / offline mode

Place inputs under:

- `knowledge/sources/gita/raw/besant_wikisource/discourse_01.html` … `discourse_18.html` (optional cache; builder can refetch)
- `knowledge/sources/mahabharata/raw/ganguli_aasi/maha01.txt` … `maha18.txt`

Then:

```powershell
python tools\rag-builder\scripts\build_all.py --normalize --index
```

If normalization cannot find raw files, the builder prints **clear errors**, writes validation reports with warnings, and still emits a **minimal empty schema** SQLite file so the repo remains consistent (Android falls back when DB has no rows).

## Outputs

| Output | Path |
|--------|------|
| Gita JSONL | `knowledge/sources/gita/processed/gita_verses.jsonl` |
| Mahabharata JSONL | `knowledge/sources/mahabharata/processed/mahabharata_chunks.jsonl` |
| SQLite | `knowledge/indexes/sarathi_rag.sqlite` |
| Android copy | `app/src/main/assets/rag/sarathi_rag.sqlite` |
| Manifests | `knowledge/indexes/sarathi_rag_manifest.json`, `build_report.json` |

## Troubleshooting

- **403 / Cloudflare on sacred-texts.com:** use Wikisource API + AASI GitHub (default) instead of scraping sacred-texts.
- **Fewer than 700 Gita verses:** check `gita_validation_report.json`—Wikisource layout changes can break verse detection; open a GitHub issue with a saved HTML sample.
- **Huge SQLite:** full Mahabharata is very large. For dev, set `mahabharata.max_parva_number` in `config/chunking.yaml` to a small integer.
