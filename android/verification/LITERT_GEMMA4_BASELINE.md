# LiteRT-LM Gemma 4 — baseline snapshot

**Date:** 2026-05-14
**Git HEAD:** `b29ec70f0845fd3c14fd8ceb576c577d8b763398`

## RAG status (from `android/app/src/main/assets/rag/sarathi_rag_manifest.json`)

| Metric | Count |
|--------|------:|
| Gita documents | 700 |
| Mahabharata documents | 3929 |
| Concepts | 15 |

No RAG schema or corpus changes were made for this LiteRT-LM integration.

## Expected local model (not in Git)

| Field | Value |
|-------|--------|
| Hugging Face / upstream id | `litert-community/gemma-4-E2B-it-litert-lm` |
| Local file (developer machine) | `local-models/gemma4-e2b/gemma-4-E2B-it.litertlm` |
| Approx size | ~2.41 GiB (2,588,147,712 bytes at last check) |

## Git ignore verification

The following patterns in `.gitignore` cover model artifacts and local drops:

- `local-models/`
- `*.litertlm`
- `*.task`
- `android/app/src/main/assets/models/`

`git check-ignore -v local-models/gemma4-e2b/gemma-4-E2B-it.litertlm` reports `local-models/` (expected).

## Workstreams

1. Baseline / Git Safety — this file.
2. LiteRT-LM Dependency + API Research — `com.google.ai.edge.litertlm:litertlm-android` + Kotlin `Engine` / `Conversation` API ([getting started](https://raw.githubusercontent.com/google-ai-edge/LiteRT-LM/main/docs/api/kotlin/getting_started.md)).
3. Model File + Device Push — see `verification/smoke_litert_gemma4_adb.ps1` and `android/docs/LITERT_GEMMA4_SETUP.md`.
4. LiteRT Runtime Adapter — `LiteRtLmGemmaChatEngine.kt`, `ModelManager` resolution, `ChatViewModel` selection.
5. Build — `:app:assembleDebug` with `JAVA_HOME` pointing at Android Studio JBR.
6. Emulator QA — install, Settings “Check model”, Chat prompts (manual + scripted helpers).
7. Documentation + Report — `android/docs/LITERT_GEMMA4_SETUP.md`, `verification/LITERT_GEMMA4_TEST_REPORT.md`.

## Sub-agent transcripts (exploration / API research)

- [Sarathi LLM codebase map](bd39eb1d-fa30-4438-9c1e-8ab1add5c3d4)
- [LiteRT-LM Android API notes](107b55a5-9b6a-4a52-bade-523c7bf9b691)
