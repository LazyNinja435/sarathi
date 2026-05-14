# Sarathi offline knowledge corpus

This folder holds the **source → raw → normalized → chunked → indexed** pipeline outputs for Sarathi’s offline scripture RAG. The Android app reads the built SQLite database from `app/src/main/assets/rag/` (see `indexes/` after a build).

## Layout

- `sources/` — provenance manifests, per-work `raw/` drops, and `processed/` JSON/JSONL.
- `indexes/` — `sarathi_rag.sqlite`, `sarathi_rag_manifest.json`, and `build_report.json` from the builder.

## Legal and licensing

Only **public-domain or clearly permissive** texts are targeted. Each source has entries in `sources/source_manifest.json` and `sources/license_manifest.json`. **Do not** bundle modern copyrighted translations. **Do not** ingest Gita Supersite or other sources until their license is explicitly verified for redistribution.

Corpus completeness is **never assumed**: validation reports flag missing sections, parse counts vs expected Gita verse totals, and AASI/Ganguli coverage gaps.

## Rebuild

From the repo root (Windows example):

```bat
py -m venv .venv
.venv\Scripts\activate
pip install -r tools\rag-builder\requirements.txt
python tools\rag-builder\scripts\build_all.py --download --normalize --index
```

Manual / offline mode: place downloaded files under the paths described in `tools/rag-builder/README.md`, then:

```bat
python tools\rag-builder\scripts\build_all.py --normalize --index
```

## Android

`tools/rag-builder/scripts/build_sqlite_index.py` copies the database to `app/src/main/assets/rag/sarathi_rag.sqlite` and syncs `sarathi_rag_manifest.json`. At runtime, `RagRepository` copies the DB to app-internal storage once and opens it read-only.

## Adding future works (Ramayana, Upanishads, Puranas)

1. Add a sibling under `knowledge/sources/future/<work>/` with `README.md` and a planned `source_manifest.json`.
2. Register the source in `tools/rag-builder/config/sources.yaml` and `rag_builder/source_registry.py`.
3. Add a `normalize_<work>.py` script and wire stages in `build_all.py`.
4. Extend `schemas.py`, `sqlite_builder.py`, and optional FTS columns only if required—prefer `metadata_json` for work-specific fields.
