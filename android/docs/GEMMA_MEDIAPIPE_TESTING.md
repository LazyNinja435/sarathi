# Gemma / MediaPipe on-device testing (Sarathi)

End-to-end checklist for validating **real** MediaPipe LLM inference while keeping **mock + RAG** stable.

## Prerequisites

- **OS:** Windows (paths below match this repo’s dev machine).
- **JDK:** `JAVA_HOME` = `C:\Program Files\Android\Android Studio\jbr`
- **Android SDK / adb:** `C:\Users\pruthvi\AppData\Local\Android\Sdk\platform-tools\adb.exe`
- **Device:** Pixel_9_Pro AVD, API 36 (`emulator-5554` when one emulator is running).
- **App id:** `com.sarathi.app`
- **Model:** MediaPipe-compatible `.task` (see `android/docs/GEMMA_MODEL_DOWNLOAD.md`). **Do not** commit the file to git.

## Build and install

```powershell
Set-Location D:\MyProjects\Sarathi
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```

Avoid `clean` unless necessary (historical flaky `mergeDebugResources` on some setups).

## Where to place the model

Sarathi resolves the first existing file (see `ModelManager.kt`):

1. Custom path from preferences (if set and file exists).
2. `files/models/` + expected filenames (`gemma.task`, `gemma3-1b-it-int4.task`, …).
3. `Download/sarathi/` + same filenames.
4. **Debug builds only:** `/data/local/tmp/llm/model_version.task` (MediaPipe sample-style adb path; may be unreadable on some devices — prefer `Download/sarathi/` or `run-as` copy).

### adb examples

```text
adb shell mkdir -p /sdcard/Download/sarathi
adb push <your-file>.task /sdcard/Download/sarathi/gemma.task
```

Optional sample-style temp path:

```text
adb shell rm -rf /data/local/tmp/llm
adb shell mkdir -p /data/local/tmp/llm
adb push <your-file>.task /data/local/tmp/llm/model_version.task
```

App-private copy (when `run-as` works on debug builds):

```text
adb shell run-as com.sarathi.app mkdir -p files/models
adb push gemma.task /sdcard/Download/sarathi/gemma.task
adb shell run-as com.sarathi.app cp /sdcard/Download/sarathi/gemma.task files/models/gemma.task
```

## In-app validation

1. Launch Sarathi.
2. Open **Settings** (drawer → Settings).
3. Tap **Check model**.
   - **Installed** — path shown; with **Use mock mode** OFF, chat uses `MediaPipeGemmaChatEngine`.
   - **Missing** — no crash; chat uses mock if mock is on or path unresolved.
4. Toggle **Use mock mode** **OFF** only after a valid `.task` is present (or to test load failure → mock fallback inside the engine).
5. In **Chat**, send: `I worked hard but failed. What does the Gita say?`  
   - Expect RAG-backed context in the prompt (see log tags); reply quality depends on model.
6. Toggle **Use mock mode** **ON** and confirm deterministic mock path still works offline.

## Logcat

```powershell
adb logcat -c
# exercise app
adb logcat -d > android\verification\gemma_mediapipe_logcat.txt
```

Useful tags: `MediaPipeGemmaChatEngine` (load / generate / fallback).

## Scripted smoke (optional)

See `verification/smoke_gemma_mediapipe_adb.ps1`.

## Known issues

- **HF gated models** return 401 without auth; follow `android/docs/GEMMA_MODEL_DOWNLOAD.md`.
- **`/data/local/tmp`** may not be readable from the app UID on physical devices; treat as emulator/dev convenience.
- **Large models** can OOM or time out on emulators; start with Gemma 3 1B int4 `.task`.
