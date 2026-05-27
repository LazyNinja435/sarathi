# Post-RAG integration plan (snapshot)

**Date:** 2026-05-14  
**Project root:** `D:\MyProjects\Sarathi`

## Git

This workspace directory is **not** a git repository (`git status` reports “not a git repository”). No commit workflow was applied.

## Objectives

- Validate Android build, RAG wiring, SQLite FTS5, and fallbacks.
- Validate and, where safe, complete Besant Gita to **700/700** canonical verses.
- Run emulator smoke where a device is available.
- Consolidate findings in `verification/POST_RAG_INTEGRATION_REPORT.md`.

## Workstreams (sub-agents)

1. Build & Android Integration — Gradle, `RagRepository`, `SarathiDatabaseProvider`, `ChatViewModel`, engines, `VerseRepository`.
2. RAG Data Validation — manifests, `build_report.json`, SQLite counts and FTS probes.
3. Gita Completeness / Parser — `gita_parser.py`, missing-verse report, normalize/index.
4. Emulator QA — `adb`, install, scripted smoke, screenshots, logcat.
5. Documentation — README, `android/docs/RAG_INTEGRATION.md`, `android/docs/ANDROID_DEV_SETUP.md`, final report.

## Execution order

Parallel read-only inspections first; minimal code/data fixes only after confirmed issues; final `assembleDebug` + smoke + report.
