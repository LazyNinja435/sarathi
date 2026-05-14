# Gemma / MediaPipe test — baseline snapshot

**Recorded:** 2026-05-14

## Git

- **Repository:** initialized at `D:\MyProjects\Sarathi` (was not a git repo before this milestone).
- **Baseline commit:** `e92c4d951869892889f9bfc63b9908b68e0ab690`

## Build status

- **Command:** `.\gradlew.bat :app:assembleDebug` with `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`
- **Result:** **SUCCESS** (after `buildConfig` + ModelManager / MediaPipe tweaks in this harness).

## RAG status (unchanged)

- **Bhagavad Gita rows:** 700  
- **Mahabharata rows:** 3929  
- **Total documents / FTS:** 4629  
- **Source:** `docs/RAG_INTEGRATION.md`, bundled `app/src/main/assets/rag/sarathi_rag.sqlite`

## Smoke status

- **Post-RAG smoke:** previously passed per project milestone notes.
- **This Gemma pass:** emulator install + Settings + chat chips + screenshots; **no** live MediaPipe generation (no `.task` on device).

## Notes

- Model binaries and `verification/gemma_mediapipe_logcat.txt` are **git-ignored**; keep captures locally only.
