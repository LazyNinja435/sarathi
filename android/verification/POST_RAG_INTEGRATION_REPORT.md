# Post-RAG integration report

**Date:** 2026-05-14
**Project:** `D:\MyProjects\Sarathi`

## Environment

| Item | Value |
|------|--------|
| OS | Windows 10 |
| JAVA_HOME (build) | `C:\Program Files\Android\Android Studio\jbr` |
| adb | `C:\Users\pruthvi\AppData\Local\Android\Sdk\platform-tools\adb.exe` |
| Emulator | `emulator-5554` — Pixel_9_Pro (AVD), API 36 |
| Git | Workspace path is **not** a git repository (`git status` unavailable). |

## Build & Android Integration Results

- **RAG copy:** `SarathiDatabaseProvider` copies `assets/rag/sarathi_rag.sqlite` to internal `filesDir/rag/` when missing; **idempotent** when the file already exists and opens cleanly.
- **Recovery:** If the internal file exists but SQLite cannot open it, the provider **deletes** it once and recopies from assets (narrow fix for corrupt/partial copies).
- **RagRepository:** `search(..., bm25)`, `getVerseOfDay`, SQL errors → empty list / null (no crash).
- **ChatViewModel:** `rag.search(userMessage, 3)` before `generateReply`; `PromptBuilder` lists citations and source titles.
- **Engines:** `ChatEngine` contract implemented in mock and MediaPipe; mock tolerates empty RAG context.
- **VerseRepository:** RAG first, then `verses.json`.
- **Build:** `:app:assembleDebug` and `:app:installDebug` **successful** on this run (`JAVA_HOME` = Android Studio JBR).

```text
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug
→ BUILD SUCCESSFUL
.\gradlew.bat :app:installDebug
→ Installed on emulator-5554 (Pixel_9_Pro AVD)
```

## RAG Data Validation Results

- Manifests and `build_report.json` / `validation_summary.json` agree on **700** Gita and **3929** Mahabharata documents.
- SQLite in `shared/knowledge/indexes` and `android/app/src/main/assets/rag` are byte-identical (**44,838,912** bytes) after index.
- `documents_fts` mirrors document count; FTS `MATCH` queries for **dharma**, **karma**, **fear**, and **Arjuna Krishna** return sensible hits. Use **`doc_id`**, not `id`, when selecting directly from `documents_fts`.

## Gita Completeness Audit

- **700 / 700** achieved in `gita_verses.jsonl` and SQLite; see prior section and `gita_missing_verses_report.json`.
- **Residual warnings:** duplicate float markers in discourse 1 (verses 21 and 27); omission of non-canonical **13.35** per 700-shloka chapter-13 count.

## Post-RAG Emulator Smoke Test

- **Script:** `verification/smoke_post_rag_adb.ps1` — **passed** on `emulator-5554`.
- **Artifacts:** screenshots under `verification/screenshots/post_rag/`; logcat `verification/post_rag_logcat.txt`.

## Sub-agent execution summary

| Workstream | Scope | Outcome |
|------------|--------|---------|
| 1 — Build & Android Integration | Gradle, manifest, RAG Kotlin, engines, verse flow | **Pass:** FTS5+bm25, top-3 retrieval, prompt citations, mock fallback; **fix:** corrupt DB one-shot recovery in `SarathiDatabaseProvider`. |
| 2 — RAG Data Validation | Manifests, JSON reports, SQLite probes | **Pass:** indexes and app assets match; `documents` 4629; `gita` 700; `mahabharata` 3929; FTS non-empty; sample MATCH queries OK (`doc_id` column on `documents_fts`). |
| 3 — Gita completeness / parser | Besant HTML, `gita_parser.py` | **700/700** canonical set; see warnings in `gita_validation_report.json` and `gita_missing_verses_report.json`. |
| 4 — Emulator QA | `installDebug`, `smoke_post_rag_adb.ps1` | **Pass** (script exit 0 ~132s). |
| 5 — Documentation | README, `docs/*`, this report | **Done.** |

## Files changed (engineering)

### Android / Kotlin

- `android/app/src/main/java/com/sarathi/app/rag/SarathiDatabaseProvider.kt` — If an internal DB file exists but `SQLiteDatabase.openDatabase` fails, delete the file once and recopy from assets.

### RAG builder (Python)

- `tools/rag-builder/src/rag_builder/gita_parser.py` — Robust verse indexing: float-right blob + optional `)` + tooltip `title` + trailing `n)`; Devanagari `॥` fallback when float text is empty; surgical fixes for Besant slips in chapters **17** and **18**; omit **13.35** for standard 700-shloka canon; `complete` is now **equality** with 700.

### Regenerated data / assets (do not hand-edit)

- `shared/knowledge/sources/gita/processed/gita_verses.jsonl`, `gita_validation_report.json`, `gita_chapters.json`, related outputs
- `shared/knowledge/indexes/sarathi_rag.sqlite`, `sarathi_rag_manifest.json`, `build_report.json`, `validation_summary.json`
- `android/app/src/main/assets/rag/sarathi_rag.sqlite`, `sarathi_rag_manifest.json`

### Verification / docs

- `verification/POST_RAG_INTEGRATION_PLAN.md` (this cycle plan)
- `verification/POST_RAG_INTEGRATION_REPORT.md` (this file)
- `verification/sqlite_rag_check.py` — repeatable SQLite smoke queries
- `verification/smoke_post_rag_adb.ps1` — post-RAG UI path + logcat
- `shared/knowledge/sources/gita/processed/gita_missing_verses_report.json` — gap / remediation notes
- `android/docs/RAG_INTEGRATION.md`, `android/docs/ANDROID_DEV_SETUP.md`
- `README.md` — Windows `JAVA_HOME`, adb path, RAG rebuild command, Pixel_9_Pro note

## Issues found

1. **692 → 700 Gita gap** — Root causes: Wikisource float markup (`(42`, `32)`, gap tooltips), two float/Sanskrit mismatches in ch.17–18, and non-canonical **13.35**.
2. **Corrupt internal SQLite** — If `filesDir/rag/sarathi_rag.sqlite` existed but was unreadable, the app never recopied from assets.
3. **Git** — Project folder not under git; no automated diff of uncommitted files.

## Fixes made

1. **`gita_parser.py`** — Safer verse extraction + documented omissions and two surgical corrections; strict `complete == 700`.
2. **`SarathiDatabaseProvider.kt`** — One-shot delete + recopy when open fails on an existing file.
3. **Rebuild pipeline** — `build_all.py --normalize --index` refreshed SQLite and Android assets.

## Open risks

- **Chapter 1 duplicates:** Two verses still have alternate English paragraphs dropped due to duplicate float markers in raw HTML; content is not double-indexed.
- **13.35 omitted:** Anyone expecting Besant’s fifth thirty-fifth line in chapter 13 in-app will not see it in RAG (by design for 700-shloka alignment).
- **FTS quoting:** User queries are passed as phrase-style quoted FTS strings; multi-token recall may differ from free `AND` queries.
- **Large bundled DB** — ~43 MB compressed assets; first launch copies once to internal storage.

## Safe to continue: Yes / No

**Yes** — `:app:assembleDebug` succeeds, emulator smoke passed, RAG DB loads with **700** Gita documents and **3929** Mahabharata chunks, mock mode remains viable without Gemma, and no internet is required for these paths.

Proceeding to **Gemma `.task` installation and on-device MediaPipe testing** is a reasonable next step when you are ready to validate non-mock inference (separate from this RAG milestone).
