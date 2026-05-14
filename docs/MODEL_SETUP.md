# Sarathi — on-device model setup (Gemma / MediaPipe)

Sarathi is designed to work with **Google MediaPipe LLM Inference** on Android using a **Gemma-family** (or other supported) model packaged as a **`.task`** file.

## Why the model is not in Git

- Model files are **large** (often hundreds of MB to multiple GB).
- Many official bundles are **terms-gated** (you must accept license terms before download).
- Keeping models out of the repo avoids accidental redistribution.

## What file to use

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

**Debug builds** also probe:

`/data/local/tmp/llm/model_version.task`

This mirrors common **MediaPipe LLM Inference** sample `adb push` flows. On physical devices the app may lack read access even if the file exists; use **Option A** or **Option B** for reliable detection. The path appears in **Settings → Expected locations** only in debug builds.

## Verify in the app

1. Install Sarathi.
2. Copy the `.task` file to one of the paths above.
3. Open **Settings → Check model**.  
   - **Installed** — MediaPipe engine will be used (unless **Use mock mode** is on).  
   - **Missing** — the app continues in **mock mode** and will not crash.

## Official references

- Google **MediaPipe** / **AI Edge** documentation for **LLM Inference** on Android (dependency: `com.google.mediapipe:tasks-genai`).
- Upstream sample: `google-ai-edge/mediapipe-samples` — `examples/llm_inference/android`.

Always follow the **exact** model + API version pairing described in the docs you use.

## TODO (later)

- **In-app downloader** for approved bundles, with clear license acceptance and integrity checks.
- Optional **GPU vs CPU** backend selection per device (see comments in `MediaPipeGemmaChatEngine.kt`).

## Hugging Face / tokens

This project **does not** embed Hugging Face tokens and does not auto-download gated models. Obtain the `.task` file through official channels, accept terms as required, then side-load manually as above.
