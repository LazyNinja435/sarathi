# Sarathi — on-device model setup (LiteRT-LM + MediaPipe)

Sarathi supports two **offline** on-device stacks:

1. **LiteRT-LM (preferred)** — Gemma-family checkpoints as a **`.litertlm`** file (`com.google.ai.edge.litertlm:litertlm-android`).  
2. **MediaPipe LLM Inference (legacy / alternate)** — bundles as a **`.task`** file (`com.google.mediapipe:tasks-genai`).

When **mock mode** is off, Sarathi **prefers `.litertlm`** if found, otherwise a compatible **`.task`**, otherwise it **falls back to mock** responses.

For Gemma 4 E2B LiteRT-LM specifically, see **`docs/LITERT_GEMMA4_SETUP.md`**.

## Why the model is not in Git

- Model files are **large** (often hundreds of MB to multiple GB).
- Many official bundles are **terms-gated** (you must accept license terms before download).
- Keeping models out of the repo avoids accidental redistribution.

## What file to use (LiteRT-LM)

Use a **`.litertlm`** bundle documented for **LiteRT-LM** / **Google AI Edge** (for example from [Hugging Face `litert-community`](https://huggingface.co/litert-community)). Sarathi searches common filenames such as:

- `gemma-4-E2B-it.litertlm`
- `gemma4-e2b.litertlm`
- `gemma.litertlm`

**Do not** commit the file to git after downloading.

## What file to use (MediaPipe)

Use a **`.task`** bundle documented as compatible with **MediaPipe Tasks GenAI** / **LLM Inference** for Android. Names Sarathi looks for (first match wins):

- `gemma.task`
- `gemma-3n.task`
- `gemma-3-1b-it.task`
- `gemma3-1b-it-int4.task` (example naming used in upstream samples; exact filenames depend on the bundle you obtain)

**Do not** commit the file to git after downloading.

### Recommended first runtime test

For **emulator and device smoke tests**, prefer the smallest reliable official MediaPipe-compatible bundle, typically **Gemma 3 1B int4** as a single `.task` (often named like `gemma3-1b-it-int4.task`). This validates wiring, memory, and load time before trying larger Gemma 2B/3n/4B artifacts.

See **`docs/GEMMA_MODEL_DOWNLOAD.md`** for Hugging Face gating (401 without auth) and **manual** download steps — never store tokens in the repo.

## Where to place the file

### Option A — App-specific storage (recommended)

Path pattern:

`Android/data/com.sarathi.app/files/models/<filename>.task`

Create the `models` folder if needed. You can copy the file with **Android Studio → Device Explorer**, or with **adb**:

```text
adb shell mkdir -p /sdcard/Android/data/com.sarathi.app/files/models
adb push gemma.task /sdcard/Android/data/com.sarathi.app/files/models/gemma.task
```

(Exact `/sdcard/...` layout can vary by device; Device Explorer shows the resolved path.)

### Option B — Public Download folder (may be restricted on newer Android)

`/sdcard/Download/sarathi/gemma.task`

On some devices or OS versions, apps cannot read arbitrary Download paths without extra permissions or a Storage Access Framework flow. If detection fails, prefer **Option A**.

### Option C — Debug-only temp path (emulator / dev)

**Debug builds** also probe MediaPipe samples paths:

`/data/local/tmp/llm/model_version.task`

and LiteRT-LM helper paths such as:

`/data/local/tmp/llm/model.litertlm`

This mirrors common **adb push** developer flows. On physical devices the app may lack read access even if the file exists; use **Option A** or **Option B** for reliable detection. Debug-only paths appear in **Settings → Expected locations** only in debug builds.

## Verify in the app

1. Install Sarathi.
2. Copy a **`.litertlm`** (preferred) or **`.task`** file to one of the paths above (see also `docs/LITERT_GEMMA4_SETUP.md`).
3. Open **Settings → Check model**.  
   - **Installed** — shows the resolved filesystem path. With mock mode off, Sarathi uses **LiteRT-LM** when a `.litertlm` is found, else **MediaPipe** for `.task`.  
   - **Missing** — the app continues with **mock** responses and will not crash.

## Official references

- **LiteRT-LM** Kotlin/Android: `com.google.ai.edge.litertlm:litertlm-android` — see [LiteRT-LM Kotlin getting started](https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md).
- Google **MediaPipe** / **AI Edge** documentation for **LLM Inference** on Android (dependency: `com.google.mediapipe:tasks-genai`).
- Upstream sample: `google-ai-edge/mediapipe-samples` — `examples/llm_inference/android`.

Always follow the **exact** model + API version pairing described in the docs you use.

## TODO (later)

- **In-app downloader** for approved bundles, with clear license acceptance and integrity checks.
- Optional **GPU vs CPU** backend selection per device (see comments in `MediaPipeGemmaChatEngine.kt`).

## Hugging Face / tokens

This project **does not** embed Hugging Face tokens and does not auto-download gated models. Obtain model files through official channels, accept terms as required, then side-load manually as above.
