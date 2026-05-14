# Sarathi

**Sarathi** (charioteer) is a native Android, offline-first spiritual companion app rooted in Bhagavad Gita wisdom. It is built with **Kotlin**, **Jetpack Compose**, and **Material 3**. The UI is calm, devotional, and premium—midnight indigo, sacred gold, and parchment tones.

## Requirements

- **Android Studio** (Iguana / Jellyfish or newer recommended) on **Windows**
- **JDK 17** (bundled with Android Studio as *jbr*)
- **Android SDK** with API **35** (compile) installed
- Physical device or emulator running **API 26+**

## Open in Android Studio

1. **File → Open** and select the `Sarathi` folder (the one containing `settings.gradle.kts`).
2. Let Gradle sync finish.
3. Choose the `app` run configuration and select a device.

## Command-line build (Windows)

- Set **`JAVA_HOME`** to JDK 17 (e.g. Android Studio’s JBR: `C:\Program Files\Android\Android Studio\jbr`).
- If **`adb`** is not on your PATH, call it explicitly, for example: `C:\Users\pruthvi\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- Create **`local.properties`** in the project root with your Android SDK path, for example:
  ```properties
  sdk.dir=C\:/Users/<you>/AppData/Local/Android/Sdk
  ```
  (Android Studio creates this automatically when you open the project.)

```powershell
.\gradlew.bat :app:assembleDebug
```

Debug APK output:

`app\build\outputs\apk\debug\app-debug.apk`

## Install on a physical device

- USB debugging enabled on the phone  
- Then either run from Android Studio, or:

```powershell
.\gradlew.bat :app:installDebug
```

(Requires `adb` on PATH and an authorized device.)

## Mock mode (default)

The app runs fully **without internet** using **MockKrishnaChatEngine**: short, keyword-aware responses in the Sarathi voice. This mode is always available and is used when:

- No compatible `.task` model file is found, or  
- **Settings → Use mock mode** is enabled.

## Offline RAG (SQLite FTS5)

- Bundled database: **`app/src/main/assets/rag/sarathi_rag.sqlite`** (manifest: `app/src/main/assets/rag/sarathi_rag_manifest.json`).
- v1 retrieval uses **SQLite FTS5** keyword search with BM25 ranking — **no embeddings** in the app bundle yet.
- Rebuild the corpus and copy fresh assets:

```powershell
py -m venv .venv
.\.venv\Scripts\activate
pip install -r tools\rag-builder\requirements.txt
python tools\rag-builder\scripts\build_all.py --download --normalize --index
```

See **`docs/RAG_INTEGRATION.md`** for how `ChatViewModel`, `PromptBuilder`, and `VerseRepository` use RAG.

## Emulator note

Smoke testing on **Pixel_9_Pro API 36** has been used for this repo; see **`docs/ANDROID_DEV_SETUP.md`** for `JAVA_HOME`, adb, and Gradle-on-Windows notes (including `kotlin.incremental=false` and avoiding unnecessary `clean`).

## On-device model (MediaPipe + Gemma `.task`)

When a compatible Gemma-family `.task` bundle is present, the app can use **MediaPipe LLM Inference** (`com.google.mediapipe:tasks-genai`) via `MediaPipeGemmaChatEngine`. Model files are **not** bundled in the repo (size + licensing).

See **[docs/MODEL_SETUP.md](docs/MODEL_SETUP.md)** for manual download, placement paths, and troubleshooting.

## Future: iOS

This repository is Android-only. A future iOS version could mirror flows with SwiftUI and an on-device inference stack appropriate for Apple platforms.

## License

Project structure and code are for your use as part of this Sarathi app; third-party libraries follow their respective licenses.
