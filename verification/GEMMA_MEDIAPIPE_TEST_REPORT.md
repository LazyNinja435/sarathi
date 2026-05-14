# Sarathi — Gemma / MediaPipe validation report

**Date:** 2026-05-14  
**Lead:** consolidated sub-agent workstreams (baseline git, model acquisition, runtime integration, build, emulator QA, documentation).

---

## Environment

| Item | Value |
|------|--------|
| Host OS | Windows 10 |
| Repo | `D:\MyProjects\Sarathi` |
| JAVA_HOME | `C:\Program Files\Android\Android Studio\jbr` |
| adb | `C:\Users\pruthvi\AppData\Local\Android\Sdk\platform-tools\adb.exe` |
| Emulator | Pixel_9_Pro API 36 (`emulator-5554`) |
| MediaPipe | `com.google.mediapipe:tasks-genai:0.10.27` (unchanged) |
| App id | `com.sarathi.app` |

---

## Sub-agent / workstream summary

| # | Workstream | Outcome |
|---|------------|---------|
| 1 | Baseline Safety / Git | Initialized new git repo; expanded `.gitignore` (models, `.task`, `local-models/`, `__pycache__/`, verification log artifacts); initial commit planned with message **Stable post-RAG Sarathi baseline**. |
| 2 | Gemma Model Acquisition | No `.task` binary downloaded: Hugging Face resolve URL returns **401** without auth/terms. Manual steps in `docs/GEMMA_MODEL_DOWNLOAD.md`. Local holding dir: `local-models/` (ignored). |
| 3 | MediaPipe Runtime Integration | `buildConfig = true`; **debug-only** resolve + UI hint for `/data/local/tmp/llm/model_version.task`; `MediaPipeGemmaChatEngine` **max tokens 512**; concise **Info** logs (basename only, output length). |
| 4 | Android Build | `:app:assembleDebug` **SUCCESS**. |
| 5 | Emulator Install & QA | `:app:installDebug` **SUCCESS**; Settings shows **Missing** model; chip-driven chat exercise; screenshots under `verification/screenshots/gemma/`; logcat capture at `verification/gemma_mediapipe_logcat.txt` (git-ignored). |
| 6 | Documentation | `docs/MODEL_SETUP.md` updated; added `docs/GEMMA_MODEL_DOWNLOAD.md`, `docs/GEMMA_MEDIAPIPE_TESTING.md`, `verification/smoke_gemma_mediapipe_adb.ps1`. |

---

## Baseline Safety Results

- **Git:** Repository was **not** initialized; `git init` performed. `.gitignore` updated per milestone (including `*.task`, `models/`, `local-models/`, `.venv/`, `verification/gemma_mediapipe_logcat.txt`, `__pycache__/`).
- **Commit:** `031024f25fdf6646f76ecc2761bc84ebc0f0464c` (matches `verification/GEMMA_TEST_BASELINE.md`).
- **Stability:** No `git clean`; no RAG rebuild; no changes to RAG corpus JSONL/SQLite sources beyond existing tracked assets.

---

## Gemma Model Acquisition Results

- **Target:** Gemma 3 1B int4 `.task` (e.g. `gemma3-1b-it-int4.task` on `litert-community/Gemma3-1B-IT`).
- **Automated download:** Not performed — `curl -I` to Hugging Face resolve URL → **401 Unauthorized** (gated).
- **Artifacts:** No `.task` committed or placed in git. Use **`local-models/`** on disk (ignored) after manual download.
- **Instructions:** See **`docs/GEMMA_MODEL_DOWNLOAD.md`**.

---

## Model path used

| Path | Role |
|------|------|
| *(none in this run)* | No `.task` present on emulator |
| `/storage/emulated/0/Download/sarathi/*.task` | Primary public side-load (when file exists) |
| `/data/user/0/com.sarathi.app/files/models/*.task` | In-app private models dir |
| `/data/local/tmp/llm/model_version.task` | **Debug-only** fallback (new) |

---

## MediaPipe Runtime Integration Results

- **Dependency:** Left at **0.10.27** (no upgrade).
- **Threading / UI:** `generateReply` uses `Dispatchers.IO`; `Mutex` protects inference/session.
- **Fallback:** Throwable → `Log.w` + `MockKrishnaChatEngine.generateReply` (unchanged pattern).
- **Logging:** Model basename, load begin/ok, generation char count; no full user text or scripture dump.
- **Tokens:** `setMaxTokens(512)` for lighter emulator load vs 1024.
- **RAG / persona:** No changes to `PromptBuilder` citation rules in this step.

---

## Build Results

```
.\gradlew.bat :app:assembleDebug
→ BUILD SUCCESSFUL
```

---

## Settings model status result

- **Observed:** **Missing** — expected with no side-loaded `.task`.
- **Check model:** Tapped during QA; UI remained consistent (no crash).
- **Debug hint list:** Includes temp path on debug APK (see `ModelManager.expectedPathHints`).

---

## Real Gemma generation result

- **Not executed** — no compatible `.task` on device in this milestone.
- **Expected when model present:** `MediaPipeGemmaChatEngine` runs; on load/inference failure, mock fallback with `Log.w`.

---

## Mock fallback result

- **Chat with mock path:** Exercise used suggestion chip **“I failed at something”** (proxy for failure/Gita-themed query) with **mock mode** default / missing model → assistant reply without crash (see screenshots `02`–`03`).
- **Mock toggle:** Approximate tap on mock switch + chip **“Teach me a verse”** captured in `04_mock_fallback.png`.

---

## Screenshots / logcat paths

| Artifact | Path |
|----------|------|
| Settings + model | `verification/screenshots/gemma/01_settings_model_status.png` |
| After Check model | `verification/screenshots/gemma/01b_after_check_model.png` |
| Chat attempt | `verification/screenshots/gemma/02_chat_gemma_attempt.png` |
| Follow-up chat | `verification/screenshots/gemma/03_chat_rag_context_response.png` |
| Mock path | `verification/screenshots/gemma/04_mock_fallback.png` |
| Logcat (local, ignored by git) | `verification/gemma_mediapipe_logcat.txt` |

---

## Files changed

- `app/build.gradle.kts` — `buildConfig = true`
- `app/.../llm/ModelManager.kt` — debug `/data/local/tmp/llm/model_version.task` resolve + hints
- `app/.../llm/MediaPipeGemmaChatEngine.kt` — logging, max tokens 512
- `.gitignore` — expanded ignores + `verification/gemma_mediapipe_logcat.txt`
- `docs/MODEL_SETUP.md` — recommended small model, Option C path, HF pointer
- `docs/GEMMA_MODEL_DOWNLOAD.md` — **new**
- `docs/GEMMA_MEDIAPIPE_TESTING.md` — **new**
- `verification/smoke_gemma_mediapipe_adb.ps1` — **new**
- `verification/GEMMA_TEST_BASELINE.md` — **new** (post-commit hash)
- `verification/screenshots/gemma/*.png` — **new** (QA captures)

---

## Issues found

1. **Gated model:** HF returns 401 without authentication; requires manual acceptance + download.
2. **`/data/local/tmp` readability:** May fail on physical devices for app UID; documented as dev/emulator convenience.
3. **Automated logcat filter:** Short test produced no `MediaPipeGemmaChatEngine` lines until real inference runs.

---

## Open risks

- First real `.task` load may OOM or timeout on emulator — start with Gemma 3 1B int4 only.
- MediaPipe / model revision skew: always match sample docs to `tasks-genai` version before scaling up.

---

## Safe to continue

| Question | Answer |
|----------|--------|
| Larger Gemma model | **Yes**, after 1B int4 path proven on target hardware. |
| Krishna persona LoRA fine-tuning | **Not yet** — establish stable base inference and evaluation harness first. |
| iOS port | **Yes to plan**, **no code in this repo step** — parity will need separate LiteRT / on-device stack research. |

---

## Acceptance checklist

- [x] App builds (`assembleDebug`).
- [x] App installs and launches on emulator.
- [x] Mock path works when model missing.
- [x] RAG unchanged in this change set.
- [x] No model binary in git.
- [x] No hard-coded tokens.
- [x] Report file exists (this document).
