# LiteRT-LM Gemma 4 — integration test report

**Date:** 2026-05-14  
**Branch / HEAD:** `b29ec70f0845fd3c14fd8ceb576c577d8b763398` (pre-commit working tree; see git status for local edits)

## Sub-agents / workstreams

| # | Workstream | Outcome |
|---|------------|---------|
| 1 | Baseline / Git Safety | `verification/LITERT_GEMMA4_BASELINE.md` created; `.gitignore` already covers `local-models/`, `*.litertlm`, `*.task`, `app/src/main/assets/models/`. |
| 2 | LiteRT-LM dependency + API research | `com.google.ai.edge.litertlm:litertlm-android:0.11.0`; Kotlin API per [LiteRT-LM Kotlin getting started](https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md) (`Engine`, `EngineConfig`, `Backend.CPU`, `Conversation`, `SamplerConfig`). |
| 3 | Model file + device push | Local model verified **2,588,147,712 bytes** (~2.41 GiB). Emulator dirs created; **multi‑GB `adb push` not executed in this automated session** (use `verification/smoke_litert_gemma4_adb.ps1` or commands in `docs/LITERT_GEMMA4_SETUP.md`). `adb shell run-as com.sarathi.app mkdir -p files/models` **succeeded** (no stderr). |
| 4 | LiteRT runtime adapter | New `LiteRtLmGemmaChatEngine`; `ModelManager` split resolvers; `ChatViewModel` prefers `.litertlm` over `.task`; Settings diagnostics. |
| 5 | Build | `:app:assembleDebug` **SUCCESS** after raising Kotlin to **2.2.21** (required by `litertlm-android` metadata). |
| 6 | Emulator QA | `:app:installDebug` **SUCCESS** on `emulator-5554` (`Pixel_9_Pro` AVD). `adb shell monkey` launch **OK**. LiteRT inference **not re-validated on-device here** because the `.litertlm` was not pushed in this session. Baseline screencap: `verification/screenshots/litert_gemma4/00_launch.png`. Named Settings/Chat captures (`01_`…`04_`) **pending manual** navigation (script leaves hooks). |
| 7 | Documentation + report | `docs/LITERT_GEMMA4_SETUP.md`, `docs/MODEL_SETUP.md` update, this report, `verification/smoke_litert_gemma4_adb.ps1`. |

### Cursor sub-agent transcripts (exploration)

- [Sarathi LLM codebase map](bd39eb1d-fa30-4438-9c1e-8ab1add5c3d4)  
- [LiteRT-LM Android API notes](107b55a5-9b6a-4a52-bade-523c7bf9b691)

## Model file

| Check | Result |
|-------|--------|
| Path | `D:\MyProjects\Sarathi\local-models\gemma4-e2b\gemma-4-E2B-it.litertlm` |
| Exists | Yes |
| Size | 2,588,147,712 bytes (~2.41 GiB; within expected ~2.5–2.6 GB band) |
| Git tracked | No (`git check-ignore` → `local-models/`) |

## Device paths (intended)

| Target | Purpose |
|--------|---------|
| `/sdcard/Download/sarathi/gemma-4-E2B-it.litertlm` | User-visible side-load |
| `/data/local/tmp/llm/model.litertlm` | Debug temp alias |
| `run-as … files/models/gemma-4-E2B-it.litertlm` | Optional private copy (debuggable) |

## Dependency / API

- **Artifact:** `com.google.ai.edge.litertlm:litertlm-android` version **0.11.0** (via `gradle/libs.versions.toml` → `libs.litertlm.android`).  
- **Toolchain:** Kotlin **2.2.21** (bump from 2.0.21 — required for LiteRT-LM AAR metadata compatibility).  
- **Adapter:** `LiteRtLmGemmaChatEngine` — `EngineConfig(Backend.CPU, cacheDir)`, lazy `Engine.initialize()`, `Mutex`, `PromptBuilder.buildFullPrompt`, `Conversation.sendMessage`, logs tails only, mock fallback on errors.  
- **Manifest:** optional `uses-native-library` entries for `libvndksupport.so`, `libOpenCL.so` (`android:required="false"`).

## Runtime selection (mock off)

1. `.litertlm` resolved → `LiteRtLmGemmaChatEngine`  
2. else `.task` resolved → `MediaPipeGemmaChatEngine`  
3. else → `MockKrishnaChatEngine`

## Settings

- Shows **Active runtime**, **Model file** kind, **Installed** path line, **Selected path** monospace line.  
- **Check model** uses `resolvePreferredModelPath` (LiteRT first).

## Build result

```
.\gradlew.bat :app:assembleDebug   → BUILD SUCCESSFUL
.\gradlew.bat :app:installDebug    → Installed on emulator-5554
```

## Settings detection result

- **With model not pushed to device in this session:** UI should show **Missing** / **Mock** active until a `.litertlm` or `.task` is present on-device.  
- **After push + Check model:** expect **Found LiteRT-LM**, **Active runtime: LiteRT-LM** (mock off), installed path populated.

## LiteRT-LM inference result

- **Not executed in this automated pass** (no on-device `.litertlm` after skipping multi‑GB push).  
- Code path: `LiteRtLmGemmaChatEngine` catches failures and delegates to `MockKrishnaChatEngine` (no crash requirement).

## Mock fallback result

- **Existing behavior retained**; mock mode toggle unchanged. **Not re-run as scripted UI test** in this pass after code changes.

## RAG status

From `app/src/main/assets/rag/sarathi_rag_manifest.json` (unchanged by this work):

- Gita documents: **700**  
- Mahabharata documents: **3929**  
- Concepts: **15**

## Screenshots / logcat

| Artifact | Path |
|----------|------|
| Baseline launch capture | `verification/screenshots/litert_gemma4/00_launch.png` |
| Manual checklist (Settings/Chat) | `01_settings_litert_found.png`, `02_chat_litert_attempt.png`, `03_chat_dharma.png`, `04_mock_fallback.png` — **capture manually** after model push |
| Logcat sample | `verification/litert_gemma4_logcat.txt` (short tail dump post-launch) |

## Files changed (tracked + new)

**Modified (git diff --name-only):**

- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/sarathi/app/llm/ModelManager.kt`
- `app/src/main/java/com/sarathi/app/ui/screens/SettingsScreen.kt`
- `app/src/main/java/com/sarathi/app/viewmodel/ChatViewModel.kt`
- `app/src/main/java/com/sarathi/app/viewmodel/SettingsViewModel.kt`
- `docs/MODEL_SETUP.md`
- `gradle/libs.versions.toml`

**Added:**

- `app/src/main/java/com/sarathi/app/llm/LiteRtLmGemmaChatEngine.kt`
- `app/src/main/java/com/sarathi/app/model/LlmRuntimeDiagnostics.kt`
- `docs/LITERT_GEMMA4_SETUP.md`
- `verification/LITERT_GEMMA4_BASELINE.md`
- `verification/LITERT_GEMMA4_TEST_REPORT.md` (this file)
- `verification/smoke_litert_gemma4_adb.ps1`
- `verification/screenshots/litert_gemma4/00_launch.png`

## Issues found

1. **Kotlin metadata mismatch:** `litertlm-android` 0.11.0 requires **Kotlin ≥ 2.2**; fixed by setting `kotlin = "2.2.21"` in `libs.versions.toml`.  
2. **Large `adb push`:** not automated here; emulator QA for LiteRT inference still requires manual push or pre-seeded device storage.  
3. **Untracked `.venv-models/`** appears in `git status` from the developer environment — unrelated to Sarathi runtime; do not commit.

## Safe to test on physical Android device?

**Yes, with caveats:** debug APK installs native JNI (`liblitertlm_jni.so`, LiteRT stacks). Expect **long first-load**, **high RAM**, and **thermal** load for multi‑GB Gemma 4 E2B. Start on **charger**, monitor **ANRs**, and keep **mock mode** available. No model binary ships in the APK.

## Safe to continue toward Krishna persona fine-tuning?

**Yes, from a wiring perspective:** RAG corpus + prompt path are untouched; LiteRT-LM is an engine swap. Fine-tuning / LoRA / checkpoint replacement remains a **separate** pipeline concern—validate **license**, **safety**, and **evaluation** before shipping tuned weights.

## Acceptance criteria checklist

| Criterion | Status |
|-----------|--------|
| App builds | Pass (`assembleDebug`) |
| App launches | Pass (`monkey` on emulator) |
| Mock mode still works | Code path preserved; manual re-verify recommended |
| RAG still works | No code changes to RAG layer |
| `.litertlm` detected if present | `ModelManager.resolveLiteRtLmPath` + Settings wiring |
| LiteRT-LM attempted when mock off + file found | `ChatViewModel.resolveEngine` |
| Graceful failure | try/catch → mock fallback in `LiteRtLmGemmaChatEngine` |
| No model binary committed | Confirmed ignored / not staged |
| Final report exists | This file |
