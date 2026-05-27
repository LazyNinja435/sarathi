# RAG integration (Sarathi v1)

## Corpus sources

- **Bhagavad Gita:** Annie Besant 4th edition via English Wikisource (`besant_wikisource_1922`), parsed from cached HTML under `shared/knowledge/sources/gita/raw/besant_wikisource/`.
- **Mahabharata:** Ganguli translation chunks (18 parvas), processed JSONL and indexed as `collection='mahabharata'`.

## Current counts (after 2026-05-14 rebuild)

| Artifact | Gita docs | Mahabharata docs | Concepts | FTS rows |
|----------|-------------|------------------|----------|----------|
| `shared/knowledge/indexes/sarathi_rag.sqlite` | 700 | 3929 | 15 | 4629 (`documents`) |
| `android/app/src/main/assets/rag/sarathi_rag.sqlite` | same (generated package copy) | | | |
| `web/apps/frontend/public/rag/sarathi_rag.json` | same source documents, JSON shape for web | | | |

Total `documents` rows: **4629** (= 700 + 3929 + 0 other collections in v1).

## Source-of-truth rule

`shared/knowledge/` is the single source of truth for Sarathi scripture knowledge.

- Raw and processed sources live under `shared/knowledge/sources/**`.
- The canonical RAG index is `shared/knowledge/indexes/sarathi_rag.sqlite`.
- Android assets under `android/app/src/main/assets/rag/` are package copies.
- Web assets under `web/apps/frontend/public/rag/` are package exports.
- Do not hand-edit Android or web RAG assets. Rebuild the corpus or run `.\android\scripts\sync-rag-assets.ps1`.
- `tools/export_web_rag_json.py` reads from `shared/knowledge/indexes/sarathi_rag.sqlite`, not from Android assets.
- `tools/verify_knowledge_artifacts.py` verifies Android DB/manifest hashes and web document counts against `shared/knowledge/indexes`.

## Gita completeness

- **700 / 700** canonical verses in `gita_verses.jsonl` and in SQLite (`gita_validation_report.json` → `complete: true`).
- Besant HTML includes an extra numbered **13.35**; the pipeline **omits** that row to align with the standard 700-shloka tally (34 verses in chapter 13). See `shared/knowledge/sources/gita/processed/gita_missing_verses_report.json`.
- Historical duplicate float markers in discourse 1 may still appear as warnings during normalize; the first occurrence is kept.

## Mahabharata coverage

See `shared/knowledge/sources/mahabharata/processed/mahabharata_validation_report.json` (18 parvas, 1877 sections, 3929 chunks at last build).

## Runtime: `RagRepository`

- `SarathiDatabaseProvider` copies `assets/rag/sarathi_rag.sqlite` to app files dir **once**; if opening the file fails (corrupt partial copy), it **deletes** the internal file and retries a single copy from assets.
- `RagRepository.warmUp()` opens the DB; `search(query, limit)` runs **FTS5** on `documents_fts` with `bm25(documents_fts)` ranking.
- Missing asset, failed copy, or SQL errors → **empty results**, not a hard crash.

## `ChatViewModel`

- On send, retrieves guidance context from the bundled DB and passes results to `ChatEngine.generateReply(..., retrievedContext = retrieved)`.

## `PromptBuilder`

- When `retrievedContext` is non-empty, appends numbered lines with **citation** and **source title**, plus a short excerpt from translation/text.

## `VerseRepository`

- `verseOfTheDay()` tries `rag.warmUp()` then `rag.getVerseOfDay()`; on failure uses bundled `verses.json`, then a small hardcoded fallback.

## Limitations (v1)

- **Keyword FTS5 only** — no embeddings, no vector rerank.
- User queries are passed through `ftsSafeQuery` as a quoted phrase; multi-token behavior differs from tokenized AND queries.
- Gita text is **public-domain Besant English** plus extracted Sanskrit blocks; not a multi-translation concordance.

## Future: embeddings / rerank

- Schema reserves space for future embedding columns or sidecar tables; plan is offline chunk embedding + optional on-device rerank after FTS recall.
