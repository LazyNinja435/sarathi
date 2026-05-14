# LiteRT-LM Gemma 4 E2B — Sarathi setup

Sarathi can run **Gemma 4 E2B** (and other compatible checkpoints) using **LiteRT-LM** with a **`.litertlm`** file. This path is **separate** from the legacy **MediaPipe** `.task` stack.

## Official dependency

Gradle (version catalog: `litertlmAndroid`):

- `implementation("com.google.ai.edge.litertlm:litertlm-android:0.11.0")`

API surface (Kotlin): `com.google.ai.edge.litertlm` — `EngineConfig`, `Engine`, `Backend`, `ConversationConfig`, `Conversation`, `SamplerConfig`. See the upstream [Kotlin getting started](https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md).

## CPU-first backend

The adapter uses `Backend.CPU()` and `cacheDir = context.cacheDir` for conservative on-device behavior. Optional GPU support can declare native libraries in the manifest (already added for OpenCL-related stacks):

```xml
<uses-native-library android:name="libvndksupport.so" android:required="false" />
<uses-native-library android:name="libOpenCL.so" android:required="false" />
```

## Obtain the model

Use an official **LiteRT-LM** `.litertlm` bundle (for example from [Hugging Face `litert-community`](https://huggingface.co/litert-community)). **Do not** commit the binary. This repository does **not** embed download tokens.

Expected local layout (ignored by Git):

`local-models/gemma4-e2b/gemma-4-E2B-it.litertlm`

## Push to emulator or device (adb)

Large files (~2.5–2.6 GiB) take time over `adb push`. From the repo root on Windows (PowerShell):

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb shell mkdir -p /sdcard/Download/sarathi
& $adb push "local-models\gemma4-e2b\gemma-4-E2B-it.litertlm" /sdcard/Download/sarathi/gemma-4-E2B-it.litertlm

& $adb shell rm -rf /data/local/tmp/llm
& $adb shell mkdir -p /data/local/tmp/llm
& $adb push "local-models\gemma4-e2b\gemma-4-E2B-it.litertlm" /data/local/tmp/llm/model.litertlm
```

### Optional: debug-only temp path

**Debug** builds also probe `/data/local/tmp/llm/model.litertlm` and `/data/local/tmp/llm/gemma-4-E2B-it.litertlm` (see `ModelManager`).

### Optional: app-private copy (`run-as`)

If the app is debuggable, you can try:

```powershell
& $adb shell run-as com.sarathi.app mkdir -p files/models
& $adb shell run-as com.sarathi.app cp /sdcard/Download/sarathi/gemma-4-E2B-it.litertlm files/models/gemma-4-E2B-it.litertlm
```

If `run-as` fails (release builds, OEM restrictions), keep the model under `Download/sarathi/` or use Android Studio Device Explorer to copy into `files/models/`.

## Resolution order inside the app

When mock mode is **off**, Sarathi prefers **LiteRT-LM** if any of these exist (first match wins):

1. Custom path from DataStore (`custom_model_path`) when it points at a `.litertlm` file  
2. `files/models/gemma-4-E2B-it.litertlm`  
3. `files/models/gemma4-e2b.litertlm`  
4. `files/models/gemma.litertlm`  
5. `/sdcard/Download/sarathi/gemma-4-E2B-it.litertlm`  
6. `/sdcard/Download/sarathi/gemma4-e2b.litertlm`  
7. `/sdcard/Download/sarathi/gemma.litertlm`  
8. Debug-only `/data/local/tmp/llm/gemma-4-E2B-it.litertlm`  
9. Debug-only `/data/local/tmp/llm/model.litertlm`  

If no `.litertlm` is found, Sarathi falls back to the existing **MediaPipe `.task`** search order, then to **mock** responses.

## Kotlin toolchain note

`litertlm-android` 0.11.0 ships Kotlin metadata newer than 2.0.x. Sarathi’s Kotlin Gradle plugin was raised to **2.2.21** so the LiteRT-LM AAR compiles cleanly.

## Smoke helpers

See `verification/smoke_litert_gemma4_adb.ps1` for adb shortcuts and screenshot output paths.
