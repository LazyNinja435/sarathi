# Sarathi

**Sarathi** (charioteer) is an offline-first spiritual companion rooted in Bhagavad Gita wisdom, available as a **native Android app** and a **web companion**.

- **Android:** Kotlin, Jetpack Compose, Material 3 — on-device Gemma 4 via LiteRT-LM.
- **Web:** React + TypeScript frontend, Fastify API, Firebase Auth — Gemini / OpenRouter cloud inference.

The UI is calm, devotional, and premium—midnight indigo, sacred gold, and parchment tones.

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

`android\app\build\outputs\apk\debug\app-debug.apk`

## Install / update from GitHub Releases

Official repo: https://github.com/LazyNinja435/sarathi

- **First install**: open the latest **GitHub Release**, download `sarathi-v<version>.apk`, and complete Android’s install prompt.
- **Check for app updates**: **Settings → Update Sarathi → Check for updates** (downloads `sarathi-latest.json`, compares `versionCode`, then optionally downloads a verified APK — still requires Android’s installer prompt).
- **Download the offline Gemma model**: **Settings → Download offline model** (chunked download from release assets into **app-private** `files/models/`).

See **`android/docs/GITHUB_RELEASE_DISTRIBUTION.md`** and **`android/docs/IN_APP_UPDATES.md`** for maintainer packaging (`dist/github-release/`) and troubleshooting.

## Release APK (signed, maintainer)

```powershell
# After exporting SARATHI_KEYSTORE_* env vars — see android/docs/ANDROID_RELEASE_SIGNING.md
.\android\scripts\build-release-apk.ps1
```

Release APK is copied to:

`dist/github-release/sarathi-v<version>.apk` (ignored by git)

## Install on a physical device

- USB debugging enabled on the phone  
- Then either run from Android Studio, or:

```powershell
.\gradlew.bat :app:installDebug
```

(Requires `adb` on PATH and an authorized device.)

## Web companion

A browser-based Sarathi experience lives alongside the Android app in this repository.

- **Frontend:** `web/apps/frontend` — React, Vite, TypeScript.
- **API:** `web/apps/api` — Fastify, TypeScript, Firebase Auth.
- **Shared packages:** prompt contract, types, and config shared across Android and web.
- **Providers:** Gemini (Google AI Studio) and OpenRouter.
- **Modes:** Demo (server key) or bring-your-own API key.
- **Deployment:** Docker Compose + Raspberry Pi (see `web/docs/PI_DEPLOYMENT.md`).

See **`web/README.md`** for local setup, shared prompt contract regeneration, and brand asset sync.

## Mock mode (default)

The app runs fully **without internet** using **MockKrishnaChatEngine**: short, keyword-aware responses in the Sarathi voice. This mode is always available and is used when:

- No compatible `.task` model file is found, or  
- **Settings → Use mock mode** is enabled.

## Offline RAG (SQLite FTS5)

- Source of truth: **`shared/knowledge/`**.
- Canonical database: **`shared/knowledge/indexes/sarathi_rag.sqlite`**.
- Android package copy: **`android/app/src/main/assets/rag/sarathi_rag.sqlite`**.
- Web package export: **`web/apps/frontend/public/rag/sarathi_rag.json`**.
- v1 retrieval uses **SQLite FTS5** keyword search with BM25 ranking — **no embeddings** in the app bundle yet.
- Rebuild the corpus and copy fresh assets:

```powershell
py -m venv .venv
.\.venv\Scripts\activate
pip install -r tools\rag-builder\requirements.txt
python tools\rag-builder\scripts\build_all.py --download --normalize --index
```

To refresh Android and web package artifacts from `shared/knowledge/indexes` without rebuilding:

```powershell
.\android\scripts\sync-rag-assets.ps1
```

See **`android/docs/RAG_INTEGRATION.md`** for how `ChatViewModel`, `PromptBuilder`, and `VerseRepository` use RAG.

## Emulator note

Smoke testing on **Pixel_9_Pro API 36** has been used for this repo; see **`android/docs/ANDROID_DEV_SETUP.md`** for `JAVA_HOME`, adb, and Gradle-on-Windows notes (including `kotlin.incremental=false` and avoiding unnecessary `clean`).

## On-device model (MediaPipe + Gemma `.task`)

When a compatible Gemma-family `.task` bundle is present, the app can use **MediaPipe LLM Inference** (`com.google.mediapipe:tasks-genai`) via `MediaPipeGemmaChatEngine`. Model files are **not** bundled in the repo (size + licensing).

See **[android/docs/MODEL_SETUP.md](android/docs/MODEL_SETUP.md)** for manual download, placement paths, and troubleshooting.

## Future: iOS

A future iOS version could mirror flows with SwiftUI and an on-device inference stack appropriate for Apple platforms.

## License

Project structure and code are for your use as part of this Sarathi app; third-party libraries follow their respective licenses.
