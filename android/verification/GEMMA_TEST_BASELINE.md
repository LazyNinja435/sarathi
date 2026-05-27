# Gemma / MediaPipe test — baseline snapshot

**Recorded:** 2026-05-14

## Git

- **Repository:** initialized at `D:\MyProjects\Sarathi` (was not a git repo before this milestone).
- **Baseline commit:** `031024f25fdf6646f76ecc2761bc84ebc0f0464c`

## Build status

- **Command:** `.\gradlew.bat :app:assembleDebug` with `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`
- **Result:** **SUCCESS** (after `buildConfig` + ModelManager / MediaPipe tweaks in this harness).

## RAG status (unchanged)

- **Bhagavad Gita rows:** 700  
- **Mahabharata rows:** 3929  
- **Total documents / FTS:** 4629  
- **Source:** `android/docs/RAG_INTEGRATION.md`, bundled `android/app/src/main/assets/rag/sarathi_rag.sqlite`

## Smoke status

- **Post-RAG smoke:** previously passed per project milestone notes.
- **This Gemma pass:** emulator install + Settings + chat chips + screenshots; **no** live MediaPipe generation (no `.task` on device).

## Notes

- Model binaries and `android/verification/gemma_mediapipe_logcat.txt` are **git-ignored**; keep captures locally only.
