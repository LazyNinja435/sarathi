# Sarathi Codebase Understanding

Date: 2026-05-18
Workspace: `D:\MyProjects\Sarathi`
Repository: `https://github.com/LazyNinja435/sarathi`

## 1. Executive summary

Sarathi is a native Android, Kotlin, Jetpack Compose spiritual companion app. The product is Android-first and offline-first, with a Krishna-inspired Bhagavad Gita voice, bundled SQLite/FTS RAG, and app-private on-device model storage. The primary intended local runtime is Gemma 4 E2B LiteRT-LM using `gemma-4-E2B-it.litertlm`; MediaPipe `.task` remains as a legacy alternate.

CONFIRMED: the repo builds at Kotlin compile level and unit tests pass in this task. CONFIRMED: current GitHub releases exist for `v0.1.0`, `v0.1.1`, and stable model tag `model-gemma-4-e2b`. CONFIRMED: latest app manifest currently resolves to APP_ONLY `v0.1.1`.

RISK: the working tree is already dirty with many modified/untracked files before this report was created, including runtime, UI, Gradle, release script, screenshots, and Google AI Studio provider files. Treat this report as describing the working tree as inspected, not necessarily a clean committed baseline.

RISK: the code currently stores a `useMockMode` preference and Settings can toggle it, but `ChatViewModel.resolveOfflineEngine()` does not appear to branch on `p.useMockMode`; it selects LiteRT, then MediaPipe, then an `unreachableEngine`. That differs from AGENTS.md's documented practice-mode-first runtime selection and should be verified before release.

## 2. Repository map

Current branch: `master`

Remote:

```text
origin  https://github.com/LazyNinja435/sarathi.git (fetch)
origin  https://github.com/LazyNinja435/sarathi.git (push)
```

High-level folders:

| Path | Purpose |
| --- | --- |
| `app/` | Android app module; all Kotlin app code, Compose UI, assets, manifest, tests. |
| `app/src/main/java/com/sarathi/app/` | Main app source packages. |
| `app/src/main/assets/rag/` | Bundled RAG SQLite DB and manifest shipped in APK assets. |
| `app/src/main/assets/verses.json` | Legacy/fallback verse-of-day asset. |
| `docs/` | Maintainer and architecture docs: model setup, RAG, updates, release signing, product design. |
| `scripts/` | Windows PowerShell build, install, package, publish, and signing helpers. |
| `tools/rag-builder/` | Python RAG corpus normalization/index tooling. |
| `tools/release/` | Python release manifest, checksum, model splitting, verification tooling. |
| `knowledge/` | RAG source, processed corpus, indexes, validation reports. Stable data; do not casually edit. |
| `verification/` | QA reports, smoke scripts, screenshots, test cases, log captures. |
| `screenshots/` | Design/reference screenshots and extracted assets. |
| `local-models/` | Local model binaries; ignored and must not be committed. |
| `dist/` | Generated release/pixel bundles; ignored and must not be committed. |
| `release-secrets/` | Local signing secrets; ignored and must not be committed. |

Ignored/generated artifacts include `.gradle/`, `.idea/`, `local.properties`, virtualenvs, `*.task`, `*.litertlm`, `models/`, `app/src/main/assets/models/`, `local-models/`, `dist/`, `verification/*.log`, APK/AAB/build outputs, keystores, `signing.properties`, and `release-secrets/`.

Generated artifacts currently present include `dist/`, `.gradle/`, `.kotlin/`, `verification/screenshots/`, RAG SQLite assets, and many screenshots. They should be preserved unless the task explicitly asks to clean them.

## 3. Build system and versioning

Single Gradle module: `:app`.

Key config:

| File | Facts |
| --- | --- |
| `settings.gradle.kts` | Root project `Sarathi`, includes `:app`, uses Google/Maven Central/Gradle Plugin Portal. |
| `build.gradle.kts` | Root plugin aliases for Android app, Kotlin Android, Kotlin Compose. |
| `gradle/libs.versions.toml` | AGP 8.7.3, Kotlin 2.2.21, Compose BOM 2024.10.00, MediaPipe GenAI 0.10.27, LiteRT-LM Android 0.11.0, AndroidX Security Crypto 1.1.0-alpha06. |
| `app/build.gradle.kts` | Namespace/applicationId `com.sarathi.app`, minSdk 26, compile/target SDK 35, Java/Kotlin 17, Compose enabled, BuildConfig enabled. |
| `sarathi-version.properties` | Current inspected version: `SARATHI_VERSION_CODE=2`, `SARATHI_VERSION_NAME=0.1.1`. |

Signing: release signing reads `SARATHI_KEYSTORE_PATH`, `SARATHI_KEYSTORE_PASSWORD`, `SARATHI_KEY_ALIAS`, and `SARATHI_KEY_PASSWORD`. If missing, Gradle release builds fall back to debug signing, which is useful for contributors but not safe for public distribution.

Manifest permissions include `INTERNET` and `REQUEST_INSTALL_PACKAGES`. There is no `MANAGE_EXTERNAL_STORAGE`. `FileProvider` exposes only `cache/updates/` for APK install handoff. Backup/data extraction rules exclude the encrypted Google AI Studio shared preferences file.

## 4. Android app architecture

Entry point:

| File | Role |
| --- | --- |
| `MainActivity.kt` | Enables edge-to-edge, creates `ChatViewModel` via app factory, sets `SarathiTheme`, hosts `SarathiNavGraph`. |
| `SarathiApp.kt` | Application object; constructs `UserPreferencesRepository`, `RagRepository`, `VerseRepository`, `DharmaRepository`, and `SarathiViewModelFactory`. |

Package map:

| Package | Purpose | Important files/classes | Dependencies and risks |
| --- | --- | --- | --- |
| `com.sarathi.app.data` | Persistence and simple repositories. | `UserPreferencesRepository`, `UserPreferences`, `VerseRepository`, `DharmaRepository`, `GoogleAiStudioApiKeyStore`. | DataStore preferences drive onboarding/tone/runtime settings; encrypted API key storage uses AndroidX Security Crypto. RISK: online API key feature changes offline-first threat model and should stay opt-in. |
| `com.sarathi.app.llm` | Chat engine abstraction and runtime adapters. | `ChatEngine`, `PromptBuilder`, `ModelManager`, `LiteRtLmGemmaChatEngine`, `MediaPipeGemmaChatEngine`, `MockKrishnaChatEngine`, `GoogleAiStudioChatEngine`, `ChatProviderSelector`. | Depends on model package, RAG result model, LiteRT-LM, MediaPipe, network client. RISK: current practice-mode preference is not honored in `ChatViewModel` selection. |
| `com.sarathi.app.model` | Small domain/status models. | `ChatMessage`, `GuidanceTone`, `Emotion`, `Verse`, `ModelStatus`, `LlmRuntimeDiagnostics`, `InstalledModelInfo`, `ModelEligibility`, `OnDeviceWisdomStatus`, `GuidanceSurface`. | Used across UI, ViewModels, runtime, and update flow. Model eligibility depends on cached release manifest and file SHA. |
| `com.sarathi.app.modeldownload` | Explicit in-app model download/install. | `ModelDownloadManager`, `ModelChunkDownloader`, `ModelInstallViewModel`, `ModelDownloadState`, `ModelDownloadAction`. | Downloads chunks to app-private temp storage, verifies chunk/full SHA, installs to `files/models`. Large downloads are not resumable. |
| `com.sarathi.app.rag` | Bundled SQLite/FTS retrieval. | `SarathiDatabaseProvider`, `RagRepository`, `RagSearchResult`. | Copies asset DB to `files/rag/`, opens read-only, returns empty results on failure. Query is phrase-quoted FTS, which may limit recall. |
| `com.sarathi.app.update` | GitHub app update manifest/download/install flow. | `ReleaseManifest`, `GithubReleaseClient`, `ManifestCache`, `AppUpdateManager`, `UpdateDownloadManager`, `ApkInstaller`, `UpdateViewModel`, `Sha256Util`. | HTTPS-only downloads, APK SHA/size/package checks, system installer prompt. Manifest cache also gates model compatibility. |
| `com.sarathi.app.ui` | Compose UI, navigation, theme, components. | `ui/navigation`, `ui/screens`, `ui/components`, `ui/theme`. | Strongly product-specific visual language; avoid generic chatbot UI. Settings has normal UX plus developer diagnostics. |
| `com.sarathi.app.viewmodel` | State and orchestration for screens. | `ChatViewModel`, `SettingsViewModel`, `OnboardingViewModel`, `VerseViewModel`, `FeelViewModel`, `DharmaViewModel`, `SarathiViewModelFactory`. | `ChatViewModel` is central coupling point for preferences, RAG, runtime selection, and messages. |

## 5. UI/navigation flows

Navigation routes:

```text
splash -> name -> tone -> blessing -> chat
chat -> verse / feel / dharma / settings
```

`SarathiNavGraph` chooses `Routes.CHAT` when `onboardingComplete` is true, otherwise `Routes.SPLASH`. It uses animated fade/scale transitions and `SacredBackground` while waiting for the initial preference read.

Main flows:

| Flow | Files | Behavior |
| --- | --- | --- |
| Splash/onboarding | `SplashScreen`, `NameScreen`, `ToneScreen`, `BlessingScreen`, `OnboardingViewModel` | Ceremonial launch, name capture, guidance tone selection, blessing screen, stores DataStore prefs. |
| Chat | `ChatScreen`, `MessageBubble`, `OfflineBadge`, `SacredTextField`, `ChatViewModel` | Main companion surface. Welcome message, drawer navigation, suggestion chips before first user message, typing state, duplicate-send lock. |
| Verse | `VerseScreen`, `VerseViewModel`, `VerseRepository` | Verse of the day from RAG when available, fallback to `verses.json`/hardcoded verse; can send reflection prompt back to chat. |
| Feel | `FeelScreen`, `FeelViewModel` | Emotion selection flow; sends "I feel ..." prompt into chat. |
| Dharma | `DharmaScreen`, `DharmaViewModel`, `DharmaRepository` | User writes a duty/reflection note; note persists in DataStore and can be reflected with chat. |
| Settings | `SettingsScreen`, `SettingsViewModel`, `UpdateViewModel`, `ModelInstallViewModel` | Calm main settings plus collapsible developer diagnostics, update flow, offline model flow, optional Google AI Studio settings, reset onboarding. |

Design language:

- Premium devotional, calm, sacred, modern.
- Midnight indigo/deep navy backgrounds.
- Warm gold accents.
- Parchment assistant cards.
- Dark translucent user bubbles.
- Subtle mandala/peacock feather motifs.
- Avoid cheap religious clipart, generic AI chrome, raw stack traces in normal UI.

Current key strings:

| Context | String |
| --- | --- |
| Loading | `The charioteer is reflecting...` |
| Input placeholder | `What rests upon your heart?` |
| Header | `Your charioteer within` |
| Settings update | `Update Sarathi` |
| Model install | `Download offline model`, `On-device wisdom: Ready` |
| Developer toggle | `Developer diagnostics` |
| Badge labels | `Google AI Studio`, `On-device wisdom`, `Vaikuntha unreachable` |

RISK: `OfflineBadge` currently labels practice/offline guidance as `Vaikuntha unreachable` in some states. That may be too alarming or too non-product-like for normal fallback UX; compare against the desired "Practice mode" / "Offline guidance" copy.

## 6. LLM runtime architecture

Chat contract:

```kotlin
suspend fun generateReply(
    userMessage: String,
    history: List<ChatMessage>,
    userName: String,
    tone: GuidanceTone,
    retrievedContext: List<RagSearchResult> = emptyList(),
): String
```

Implemented engines:

| Engine | Runtime |
| --- | --- |
| `LiteRtLmGemmaChatEngine` | Preferred on-device `.litertlm`; lazy `Engine.initialize()`, CPU backend, per-request conversation, mutex, fallback on failure. |
| `MediaPipeGemmaChatEngine` | Legacy/alternate `.task`; lazy `LlmInference`, session per request, fallback on failure. |
| `MockKrishnaChatEngine` | Deterministic scripted offline practice/fallback engine; app-safe and RAG-aware. |
| `GoogleAiStudioChatEngine` | Optional online provider using user-provided API key and `gemini-flash-lite-latest`; falls back to offline engine on errors. |
| `unreachableEngine` inside `ChatViewModel` | Returns `Vaikuntha is unreachable right now! Please check your connection settings.` |

Documented intended runtime selection in AGENTS.md:

1. Practice mode ON -> `MockKrishnaChatEngine`.
2. Practice mode OFF + compatible `.litertlm` -> `LiteRtLmGemmaChatEngine`.
3. Practice mode OFF + `.task` only -> `MediaPipeGemmaChatEngine`.
4. No model / blocked / runtime failure -> fall back gracefully to practice mode.

Observed current working-tree selection in `ChatViewModel`:

1. If Google AI Studio is enabled and API key is configured -> `GoogleAiStudioChatEngine(fallback = offlineEngine)`.
2. Offline engine checks LiteRT path.
3. If LiteRT exists but `ModelEligibility.shouldBlockLiteRt` is true -> `unreachableEngine`.
4. Else LiteRT exists -> `LiteRtLmGemmaChatEngine`.
5. Else MediaPipe `.task` exists -> `MediaPipeGemmaChatEngine`.
6. Else -> `unreachableEngine`.

RISK: `useMockMode` exists in preferences and Settings, and `MockKrishnaChatEngine` exists, but the inspected `ChatViewModel` does not use `p.useMockMode` and does not instantiate `MockKrishnaChatEngine` in the current runtime path. This is a likely behavioral regression from the intended practice-mode fallback.

LiteRT-LM:

| Item | Value |
| --- | --- |
| Expected filename | `gemma-4-E2B-it.litertlm` |
| Expected size | `2588147712` bytes |
| App-private runtime path | `files/models/gemma-4-E2B-it.litertlm` |
| Runtime class | `LiteRtLmGemmaChatEngine` |
| Validated model line | `litert-community/gemma-4-E2B-it-litert-lm` |
| Proof logs | `LiteRT-LM load ok`, `LiteRT-LM generation start`, `LiteRT-LM generation end` |

`ModelManager` still searches public `Download/sarathi/` and debug `/data/local/tmp/llm/` paths for development/status, but the required production install path is app-private `files/models/`. Public Downloads is not preferred because LiteRT native loading can hit permission denied on some device/path combinations.

`InstalledModelInfo` persists metadata to `files/models/installed-model.json` after verification: id, version, filename, size, SHA-256, install timestamp, runtime. `ModelEligibility` uses this metadata plus cached release manifest to block incompatible LiteRT files when the app manifest says the model is unsupported or `requiresModelUpdate` is true and SHA/size do not match.

Prompt flow:

| Function | Role |
| --- | --- |
| `PromptBuilder.PERSONA` | Main Sarathi persona and safety guardrails: Krishna-inspired, not literal deity, no fabricated Sanskrit/verse numbers. |
| `buildFullPrompt()` | Used by local engines; includes persona, user name, tone hint, retrieved context, recent history, latest message, 120-word reply instruction. |
| `buildSystemInstruction()` | Used by Gemini request mapper; same persona/tone/context as system instruction. |
| `GeminiRequestMapper` | Builds GenerateContent request with recent history as contents and RAG in system instruction. |

## 7. RAG architecture

Bundled app assets:

| Path | Purpose |
| --- | --- |
| `app/src/main/assets/rag/sarathi_rag.sqlite` | SQLite FTS5 RAG database in APK assets. |
| `app/src/main/assets/rag/sarathi_rag_manifest.json` | Small manifest with counts and generation date. |

Source/build corpus:

| Path | Purpose |
| --- | --- |
| `knowledge/sources/` | Raw and processed source texts. |
| `knowledge/indexes/` | Built SQLite, manifest, build and validation reports. |
| `tools/rag-builder/` | Python scripts/configs for download, normalize, validate, and index. |

CONFIRMED counts from manifests/reports:

| Collection | Count |
| --- | ---: |
| Bhagavad Gita documents | 700 |
| Mahabharata documents/chunks | 3929 |
| Concepts | 15 |
| Total documents | 4629 |

Runtime:

- `SarathiDatabaseProvider` copies `assets/rag/sarathi_rag.sqlite` into `files/rag/sarathi_rag.sqlite` on first open and opens it read-only.
- If the copied DB exists but cannot open, it deletes and recopies once.
- `RagRepository.search()` uses SQLite FTS5 `documents_fts MATCH ?` joined to `documents`, ranked by `bm25(documents_fts)`.
- `ftsSafeQuery()` wraps the whole user query as a quoted phrase. RISK: this can reduce recall for natural multi-token queries compared with tokenized AND/OR strategies.
- Failures return empty results/null rather than crashing.

Chat integration:

- `ChatViewModel.sendUserMessage()` calls `rag.search(trimmed, limit = 3)`.
- Retrieved results are passed to `ChatEngine.generateReply()`.
- `PromptBuilder` includes citations, source title, and excerpt when context exists.
- `MockKrishnaChatEngine` appends an "Inspired by" citation list when RAG results exist.

Verse of the Day:

- `VerseRepository.verseOfTheDay()` tries `rag.warmUp()` then `rag.getVerseOfDay()`.
- It falls back to `verses.json`, then to a hardcoded Bhagavad Gita 2.47 teaching.

Do not modify RAG data unless the task explicitly requires RAG work. This task did not modify RAG.

## 8. Model storage and installation

Required runtime model location:

```text
files/models/gemma-4-E2B-it.litertlm
```

In-app download flow:

1. `ModelInstallViewModel` starts from latest app manifest URL unless overridden.
2. `ModelDownloadManager.resolveManifestForModelDownload()` uses cached app manifest if valid, otherwise fetches app manifest.
3. If app manifest has inline `model.chunks`, it uses those.
4. If chunks are empty, it fetches `modelSource.externalManifestUrl` or the built-in stable model URL.
5. Chunks download sequentially into `files/model-downloads/`.
6. `ModelChunkDownloader` verifies each chunk SHA-256 and size.
7. Chunks concatenate to `files/models/<file>.tmp`.
8. Full model SHA-256 and size are verified.
9. Temp file is renamed/copied to `files/models/gemma-4-E2B-it.litertlm`.
10. `InstalledModelInfo.persistAfterVerification()` writes `installed-model.json`.
11. Temp chunks are deleted.

Space check: required free space is assembled model size + sum of chunks + 64 MiB. For the current model that can mean roughly 5+ GiB transient space.

Deleting app data (`pm clear`) or uninstalling deletes app-private model files. Normal APK updates do not clear `files/models`.

## 9. GitHub release/update architecture

Manifest URLs:

| Manifest | URL |
| --- | --- |
| App latest | `https://github.com/LazyNinja435/sarathi/releases/latest/download/sarathi-latest.json` |
| Stable model | `https://github.com/LazyNinja435/sarathi/releases/download/model-gemma-4-e2b/model-latest.json` |

Release/update code:

| File | Role |
| --- | --- |
| `ReleaseManifest.kt` | Parses app/model/release/modelSource sections and release type: APP_ONLY, FULL_MODEL, MODEL_ONLY. |
| `GithubReleaseClient.kt` | HTTPS-only text/file download via `HttpURLConnection`; follows redirects. |
| `AppUpdateManager.kt` | Compares remote `versionCode` with `BuildConfig.VERSION_CODE`. |
| `UpdateDownloadManager.kt` | Downloads APK to `cacheDir/updates/sarathi-update.apk`; verifies APK SHA, size, package name. |
| `ApkInstaller.kt` | Uses `FileProvider` content URI and Android installer prompt; opens unknown-source settings when needed. |
| `ManifestCache.kt` | Saves last app manifest JSON to `files/cache/last-release-manifest.json`; used by model eligibility. |

Scripts/tools:

| Path | Role |
| --- | --- |
| `scripts/package-github-release.ps1` | Builds APP_ONLY/FULL_MODEL/MODEL_ONLY assets; requires local model even for APP_ONLY metadata. |
| `scripts/publish-github-release.ps1` | Publishes assets with `gh`; app/full releases use `--latest`, model-only uses `--latest=false`. |
| `tools/release/split_model.py` | Splits `.litertlm` into `.partNNN` chunks, default 943,718,400 bytes. |
| `tools/release/create_release_manifest.py` | Writes `sarathi-latest.json` or `model-latest.json`. |
| `tools/release/verify_release_manifest.py` | Verifies APK/chunks and reconstructed model SHA. |
| `tools/release/write_checksums.py` | Writes checksum list. |

Verified GitHub state in this task via `gh release list/view`:

| Release | Tag | Published | Assets | Notes |
| --- | --- | --- | --- | --- |
| Sarathi v0.1.1 | `v0.1.1` | 2026-05-15T18:31:31Z | `sarathi-v0.1.1.apk`, `sarathi-latest.json`, `checksums.sha256`, `RELEASE_NOTES.md` | Latest app release. APP_ONLY manifest. |
| Sarathi v0.1.0 | `v0.1.0` | 2026-05-14T22:11:33Z | `sarathi-v0.1.0.apk`, `sarathi-latest.json`, `checksums.sha256`, `RELEASE_NOTES.md` | Prior app release. |
| Sarathi offline model | `model-gemma-4-e2b` | 2026-05-14T21:53:51Z | `model-latest.json`, `checksums.sha256`, `gemma-4-E2B-it.litertlm.part001..003` | Stable model release. Live JSON reports `FULL_MODEL` as legacy type; parser accepts it. |

Latest app manifest as fetched in this task:

- `release.tag`: `v0.1.1`
- `release.releaseType`: `APP_ONLY`
- `app.versionCode`: `2`
- `app.versionName`: `0.1.1`
- `apkFileName`: `sarathi-v0.1.1.apk`
- `model.chunks`: empty
- `modelSource.externalManifestUrl`: stable model manifest URL

Stable model manifest as fetched in this task:

- `release.tag`: `model-gemma-4-e2b`
- `release.releaseType`: `FULL_MODEL` (legacy catalog value)
- `model.fileName`: `gemma-4-E2B-it.litertlm`
- `model.sizeBytes`: `2588147712`
- `model.chunks`: three parts, sizes `943718400`, `943718400`, `700710912`

## 10. App-only vs full/model release behavior

| Type | Contents | Model chunks? | When to use | Expected model behavior |
| --- | --- | --- | --- | --- |
| APP_ONLY | APK, `sarathi-latest.json`, checksums, notes. Manifest still carries model metadata and external model URL. | No inline chunks. | UI fixes, bug fixes, app logic, compatible app update. | Preserve compatible app-private model. Do not force 2.6 GB redownload. Fresh installs can use external model manifest. |
| FULL_MODEL | APK, app manifest, checksums, notes, model chunks. | Yes, inline in app manifest. | App release tied to model/compatibility update. | App may offer/require explicit model download based on manifest policy. |
| MODEL_ONLY | `model-latest.json`, checksums, model chunks. No APK. | Yes. | Stable model catalog for fresh installs and model-only refreshes. | Downloaded separately from app update flow, explicit user action. Should not be marked GitHub Latest over app line. |

Why app-only updates preserve model: Android upgrades with the same package and signing key keep `filesDir`. Sarathi's updater downloads only the APK to cache and does not delete `files/models`.

What deletes the model: app uninstall, `pm clear`, manual deletion under app-private files, failed/corrupt replacement during model install after existing final file deletion. The current download flow assembles/verifies temp before replacing final file, which reduces replacement risk.

## 11. Security and signing

CONFIRMED in code/config:

- No model binary or release artifact should be committed; `.gitignore` covers common paths and extensions.
- GitHub tokens are not embedded in the app release/update code.
- Manifest/APK/chunk downloads require HTTPS.
- APK install is user-initiated and goes through Android installer UI.
- APK SHA-256, APK size, and package name are verified before install prompt.
- Model chunk SHA-256/size and full model SHA-256/size are verified before final install.
- No `MANAGE_EXTERNAL_STORAGE` permission.
- EncryptedSharedPreferences is used for the optional Google AI Studio API key.
- Backup rules exclude the encrypted API key preferences file.

RISK: if an attacker controls GitHub release assets/manifest, the app trusts that manifest subject to HTTPS and SHA values inside the same manifest. Future manifest signing/pinning would reduce this supply-chain risk.

RISK: release builds fall back to debug signing when signing env vars are missing. That is documented, but public release packaging must be done with maintainer signing env vars.

## 12. Tests and verification

Unit tests present:

- `GoogleAiStudioSettingsTest`
- `ChatProviderSelectorTest`
- `GeminiApiModelsTest`
- `GoogleAiStudioChatEngineTest`
- `MockKrishnaChatEngineTest`
- `PromptBuilderTest`
- `ReleaseManifestParseTest`

Instrumented tests present:

- `GoogleAiStudioApiKeyStoreInstrumentedTest`
- `ModelManagerInstrumentedTest`

Manual test cases:

- `verification/test-cases/SARATHI_GITHUB_RELEASE_UPDATE_TEST_CASES.md`
- `verification/test-cases/SARATHI_PIXEL_UX_AND_GEMMA_TEST_CASES.md`
- `verification/test-cases/SARATHI_GOOGLE_AI_STUDIO_QA.md`

Commands run in this task:

| Command | Result |
| --- | --- |
| `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:compileDebugKotlin` | PASS: BUILD SUCCESSFUL in 2s; 15 actionable tasks, 4 executed, 11 up-to-date. |
| `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:testDebugUnitTest` | PASS: BUILD SUCCESSFUL in 1s; 24 actionable tasks, 24 up-to-date. |
| `adb devices` | PASS: only `emulator-5554 device` attached. |

`connectedDebugAndroidTest` was not run in this task because no physical Pixel was attached; only an emulator was connected, and Gemma/LiteRT validation should not rely on emulator alone.

## 13. Known successful validation evidence

From existing verification reports, not re-executed in this task:

| Evidence | Status |
| --- | --- |
| `verification/PIXEL_INSTALL_REPORT.md` | Documents Pixel 10 Pro XL install, assemble/test/connected test pass, launch pass on 2026-05-14. |
| `verification/LITERT_GEMMA4_TEST_REPORT.md` | Documents LiteRT-LM integration, build pass, emulator launch, and intended physical validation markers. It says inference was not executed in that automated pass. |
| `verification/litert_gemma4_logcat.txt` | Existing logcat artifact. Review before claiming device inference for a specific build. |
| `verification/UX_POLISH_REPORT.md` | Documents UX polish and claims assemble/unit/connected tests passed on Pixel; says Gemma end-to-end was not re-executed in that slice. |
| `verification/POST_RAG_INTEGRATION_REPORT.md` | Documents RAG DB counts, emulator smoke, and RAG integration behavior after RAG rebuild. |
| `verification/GITHUB_RELEASE_BOOTSTRAP_REPORT.md` | Documents initial release bootstrap state; now superseded in part by current live GitHub releases verified in this task. |

Do not claim current Gemma inference works on device unless a current run shows `LiteRT-LM load ok`, `LiteRT-LM generation start`, and `LiteRT-LM generation end` on the target device/build.

## 14. Current gaps and risks

CONFIRMED:

- Android app code compiles in debug Kotlin.
- Debug unit tests pass.
- RAG assets are present and manifests report 700 Gita docs + 3929 Mahabharata docs.
- GitHub app releases `v0.1.0` and `v0.1.1` exist.
- GitHub stable model release `model-gemma-4-e2b` exists with three chunks.
- Latest app manifest points to APP_ONLY `v0.1.1` and external model manifest.

LIKELY:

- App-only updates preserve `files/models` because Android app upgrades preserve app-private data and updater code does not delete models.
- Fresh installs can download the model from the external model manifest if network/storage permit.
- MediaPipe `.task` path remains wired as fallback after LiteRT path.

UNVERIFIED:

- Current dirty working tree on a physical Pixel.
- Current Google AI Studio UI/API flow on device.
- Current in-app model download end-to-end from GitHub in this task.
- Current LiteRT-LM generation from app-private `files/models` in this task.
- Full release packaging scripts against local model in this task.
- Whether `v0.1.1` APK was smoke-tested after publication.

BLOCKED:

- Physical Pixel QA was not run because only `emulator-5554` was attached.
- Gemma inference was not run by design; this task avoided heavy model inference/download.

RISK:

- Practice/mock mode preference appears disconnected from `ChatViewModel` engine selection.
- Fallback currently returns an "unreachable" message instead of practice guidance when no local/online engine exists.
- Working tree contains many pre-existing changes and untracked files; future agents must inspect diffs before editing.
- Optional online Google AI Studio mode broadens privacy/security/product positioning; keep it opt-in and clearly labeled.
- RAG FTS phrase quoting may under-retrieve for natural questions.
- Large model download lacks resume support.
- Live model-only manifest still says `FULL_MODEL`; parser accepts this, but docs/tooling now prefer `MODEL_ONLY`.
- Release trust depends on GitHub/TLS and unsigned JSON manifests.

## 15. Recommended next tasks

1. Fix or intentionally redesign practice-mode runtime selection: ensure `useMockMode` routes to `MockKrishnaChatEngine`, and no-model/runtime-failure paths fall back to practice guidance rather than a hard unreachable message unless that is deliberate.
2. Run physical Pixel QA on `57171FDCQ00AZ7` or another real Pixel: install current build, verify app-private model size, practice mode, LiteRT logs, RAG prompts, Settings states.
3. Smoke-test GitHub release flows on device: latest manifest check, APP_ONLY update preservation, fresh install external model manifest, model download verification, and installer prompt.
4. Review and settle the Google AI Studio feature: product copy, privacy posture, API key storage, offline-first defaults, tests, and AGENTS.md update once committed.
5. Clean up repo hygiene before PR/release: separate generated screenshot churn from source changes, ensure no secrets/binaries/build artifacts are staged, and update docs that reflect committed behavior.

## 16. Important commands

Build/test:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Physical Pixel / adb:

```powershell
$adb="C:\Users\pruthvi\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb devices
& $adb -s 57171FDCQ00AZ7 shell "run-as com.sarathi.app stat -c '%s' files/models/gemma-4-E2B-it.litertlm"
& $adb -s 57171FDCQ00AZ7 logcat -d | findstr /i "LiteRtLmGemmaChatEngine"
```

Release packaging:

```powershell
.\scripts\build-release-apk.ps1
.\scripts\package-github-release.ps1 -ReleaseType APP_ONLY
.\scripts\package-github-release.ps1 -ReleaseType FULL_MODEL
.\scripts\package-github-release.ps1 -ReleaseType MODEL_ONLY
python tools\release\verify_release_manifest.py --dist-dir dist\github-release
python tools\release\verify_release_manifest.py --dist-dir dist\github-model-release
```

GitHub release inspection:

```powershell
gh release list --repo LazyNinja435/sarathi --limit 20
gh release view v0.1.1 --repo LazyNinja435/sarathi
gh release view model-gemma-4-e2b --repo LazyNinja435/sarathi
```

RAG inspection:

```powershell
python verification\sqlite_rag_check.py
python tools\rag-builder\scripts\build_all.py --download --normalize --index
```

Only run the RAG rebuild when the task explicitly requires RAG data changes.

## 17. Files future agents should read first

Read these before substantive work:

- `AGENTS.md`
- `README.md`
- `app/build.gradle.kts`
- `sarathi-version.properties`
- `app/src/main/java/com/sarathi/app/viewmodel/ChatViewModel.kt`
- `app/src/main/java/com/sarathi/app/llm/PromptBuilder.kt`
- `app/src/main/java/com/sarathi/app/llm/ModelManager.kt`
- `app/src/main/java/com/sarathi/app/llm/LiteRtLmGemmaChatEngine.kt`
- `app/src/main/java/com/sarathi/app/update/ReleaseManifest.kt`
- `app/src/main/java/com/sarathi/app/modeldownload/ModelDownloadManager.kt`
- `app/src/main/java/com/sarathi/app/model/InstalledModelInfo.kt`
- `docs/GITHUB_RELEASE_DISTRIBUTION.md`
- `docs/IN_APP_UPDATES.md`
- `docs/PRODUCT_DESIGN.md`
- `docs/RAG_INTEGRATION.md`
- `verification/test-cases/`

Also read these if working on the current dirty tree:

- `app/src/main/java/com/sarathi/app/llm/ChatProviderSelector.kt`
- `app/src/main/java/com/sarathi/app/llm/GoogleAiStudioChatEngine.kt`
- `app/src/main/java/com/sarathi/app/llm/GeminiApiModels.kt`
- `app/src/main/java/com/sarathi/app/data/GoogleAiStudioApiKeyStore.kt`
- `verification/test-cases/SARATHI_GOOGLE_AI_STUDIO_QA.md`
