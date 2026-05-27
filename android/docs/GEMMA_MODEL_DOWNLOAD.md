# Gemma `.task` model download (manual)

Sarathi does **not** embed Hugging Face tokens or automate gated downloads. Obtain a MediaPipe-compatible **Gemma 3 1B int4** `.task` bundle yourself, then side-load it using `android/docs/MODEL_SETUP.md` and `android/docs/GEMMA_MEDIAPIPE_TESTING.md`.

## Recommended first test artifact

| Property | Value |
|----------|--------|
| **Family** | Gemma 3 1B Instruct |
| **Format** | Single-file **`.task`** for MediaPipe Tasks GenAI **LLM Inference** |
| **Example upstream name** | `gemma3-1b-it-int4.task` |
| **Typical host** | Hugging Face repo `litert-community/Gemma3-1B-IT` (gated) |

**Why this size first:** runtime validation only — lower RAM, faster cold load, fewer emulator OOMs than 2B/4B bundles.

## Hugging Face (gated) — verified behavior

An unauthenticated `GET` to the resolve URL returns **401 Unauthorized** (gated asset). You must:

1. Create or sign in to a Hugging Face account.
2. Open the model card for **`litert-community/Gemma3-1B-IT`** (or the official successor your MediaPipe docs point to).
3. Accept any **license / terms** buttons shown on the card.
4. Create a **read** token under [Hugging Face token settings](https://huggingface.co/settings/tokens) (do **not** paste tokens into Sarathi source or repo files).
5. Download locally using one of:
   - **Browser:** download `gemma3-1b-it-int4.task` after you are logged in and terms are accepted.
   - **`huggingface-cli`** (if installed): `huggingface-cli download litert-community/Gemma3-1B-IT gemma3-1b-it-int4.task --local-dir ./local-models` (run only on your machine; authenticate when prompted).

6. Optionally rename to **`gemma.task`** for a single canonical side-load name (Sarathi accepts either name; see `ModelManager.EXPECTED_FILENAMES`).

## Do not commit binaries

Keep the file under **`local-models/`** (git-ignored at repo root) or any folder outside git, then `adb push` per testing docs.

## Alternatives

- Follow **google-ai-edge/mediapipe-samples** (`examples/llm_inference/android`) for the exact `.task` revision paired with your `tasks-genai` version.
- **Google AI Edge Gallery** / official blogs may ship or reference updated bundles; prefer sources that explicitly state MediaPipe LLM Inference compatibility.
