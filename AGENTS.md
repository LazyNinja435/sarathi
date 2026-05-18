# AGENTS.md — Sarathi

**Repository:** https://github.com/LazyNinja435/sarathi  
**Local path:** `D:\MyProjects\Sarathi`

This file is the main operating manual for coding agents working on **Sarathi**. Read it before making substantive changes.

---

## 1. Project identity

**Sarathi** is an **Android-first, offline** Krishna / Bhagavad Gita companion app.

It should feel like a **calm, premium, devotional** spiritual companion—not a generic chatbot.

The experience aims to give the emotional sense of speaking with Krishna as charioteer and friend, while the product frames itself safely as:

> **A Krishna-inspired spiritual guide rooted in the Bhagavad Gita.**

**Product guardrails**

- Do **not** make the app claim it is literally Krishna.
- Do **not** fabricate exact Sanskrit text or verse numbers.
- If no exact retrieved source supports a citation, describe the teaching as **Gita-inspired** guidance.

| Item | Value |
|------|--------|
| **App name** | Sarathi |
| **Package** | `com.sarathi.app` |
| **Primary LLM runtime** | Gemma 4 E2B **LiteRT-LM** (`.litertlm`) |
| **Legacy runtime** | MediaPipe `.task` support remains as alternate / legacy |

---

## 2. Current technical state

- **Platform:** Android native app.
- **Language:** Kotlin.
- **UI:** Jetpack Compose, **Material 3**.
- **Preferences:** DataStore.
- **RAG:** App-bundled asset **SQLite** (FTS-based retrieval).
- **LiteRT-LM:** Validated on a **physical Pixel** device.
- **Practice / mock mode:** Works end-to-end; always available as fallback.
- **MediaPipe `.task`:** Code path exists; treat as **legacy** when LiteRT is available.
- **Google AI Studio:** Optional, user-enabled online provider path exists in current code; keep it opt-in, store API keys securely, and preserve offline-first defaults.
- **GitHub release / update infrastructure:** Being extended (manifests, APK, chunked model).
- **Local workflow:** A Pixel-oriented bundle script exists for app + model install on device.

**Validated model (Hugging Face)**

- Repo / identifier: `litert-community/gemma-4-E2B-it-litert-lm`
- On-device filename: **`gemma-4-E2B-it.litertlm`**
- Expected size: **2 588 147 712 bytes** (exact byte count matters for integrity checks).

**Runtime model location (required)**

- Store the model in **app-private** storage:  
  **`files/models/gemma-4-E2B-it.litertlm`**
- **Do not** use public **Downloads** as the primary runtime path—LiteRT native loading can hit **permission denied** on some devices / paths.

**Known-good Pixel log markers**

- `LiteRT-LM load ok`
- `LiteRT-LM generation start`
- `LiteRT-LM generation end`

---

## 3. Design language

**Tone:** Premium devotional—**calm, sacred, modern**.

**Visual language**

- **Midnight indigo / deep navy** backgrounds.
- **Warm gold** accents.
- **Parchment** assistant cards.
- **Dark translucent** user message bubbles.
- Subtle **mandala / peacock feather** inspiration (tasteful, not loud).
- **Large, readable** typography.
- **No** cheap religious clipart, clutter, or generic “AI chat” chrome.

**User-facing copy examples**

| Context | Example |
|---------|---------|
| Loading | “The charioteer is reflecting…” |
| Input placeholder | “What rests upon your heart?” |
| Runtime / trust labels | “On-device wisdom”, “Offline guidance”, “Practice mode” |

**Wording**

- Avoid showing **“mock”** prominently to typical users; prefer **“practice mode”** in UI.

---

## 4. Core architecture

**Base package:** `app/src/main/java/com/sarathi/app/`

### `llm/`

- `ChatEngine` (abstraction)
- `MockKrishnaChatEngine` (practice mode)
- `LiteRtLmGemmaChatEngine` (preferred on-device Gemma path)
- `MediaPipeGemmaChatEngine` (legacy `.task`)
- `ModelManager`
- `PromptBuilder`

### `rag/`

- SQLite / FTS-based retrieval.
- Corpus includes **Bhagavad Gita** and **Mahabharata**-sourced content (see §5 for counts).

### `model/`

- `LlmRuntimeDiagnostics`
- `GuidanceSurface`
- `InstalledModelInfo`
- `ModelEligibility`
- `OnDeviceWisdomStatus`

### `modeldownload/`

- `ModelDownloadManager`
- Chunked download, **SHA** verification, install into **app-private** storage.

### `update/`

- `ReleaseManifest`
- GitHub release update client
- APK update flow (user-initiated, verified)
- Manifest caching

### `ui/`

- Compose screens, components, theme.

### `viewmodel/`

- `ChatViewModel`
- `SettingsViewModel`
- `UpdateViewModel`
- `ModelInstallViewModel`

### Runtime selection (priority)

1. **Practice mode ON** → `MockKrishnaChatEngine`.
2. **Google AI Studio enabled** + configured user API key → `GoogleAiStudioChatEngine` with offline/practice fallback.
3. **Practice mode OFF** + compatible **`.litertlm`** → `LiteRtLmGemmaChatEngine`.
4. **Practice mode OFF** + **`.task` only** → `MediaPipeGemmaChatEngine`.
5. **No model**, **blocked / incompatible model**, or **runtime failure** → fall back gracefully to **practice mode**. **Do not crash.**

**Preference:** When both LiteRT and MediaPipe models exist, **prefer LiteRT** over MediaPipe.

---

## 5. RAG rules

RAG is **stable**; do **not** casually modify bundled corpora or rebuild assets unless the task explicitly requires RAG work.

**Approximate current corpus (as documented for agents)**

- **700** Bhagavad Gita rows.
- **3929** Mahabharata chunks.

**Bundled DB path**

- `app/src/main/assets/rag/sarathi_rag.sqlite`

**Prompting**

- `PromptBuilder` should include **retrieved context** when available.
- **Never** fabricate exact verse citations from thin air.
- If retrieval does not support a precise citation, phrase output as **Gita-inspired** guidance.

---

## 6. Model and release strategy

Three release **concepts**:

| Type | Contents | Typical use |
|------|-----------|-------------|
| **APP_ONLY** | APK only | UI fixes, bug fixes, app logic; **must preserve** existing app-private model; **must not** force a ~2.6 GB re-download when the installed model remains compatible. |
| **FULL_MODEL** | APK + model chunks | Model change or **compatibility** change. |
| **MODEL_ONLY** | Model manifest + model chunks | Stable model line so fresh installs can obtain the model even when the latest app release is **APP_ONLY**. |

**Default strategy**

- Publish a **stable model** release track: **`model-gemma-4-e2b`**.
- Ship **frequent app** releases (`v0.1.0`, `v0.1.1`, `v0.1.2`, …), mostly **APP_ONLY**.

**Manifest URLs**

- **App latest:**  
  `https://github.com/LazyNinja435/sarathi/releases/latest/download/sarathi-latest.json`
- **Model latest:**  
  `https://github.com/LazyNinja435/sarathi/releases/download/model-gemma-4-e2b/model-latest.json`

**GitHub “Latest” release (which release owns `/latest/download/…`)**

- `scripts/publish-github-release.ps1` passes **`--latest=true`** when creating **APP_ONLY** / **FULL_MODEL** tags so `releases/latest/download/sarathi-latest.json` tracks the **app** line.
- **MODEL_ONLY** creates use **`--latest=false`** so the stable model tag is not promoted over the app as the repo default “Latest” release.
- If a model release was ever marked Latest by mistake, run `gh release edit model-gemma-4-e2b --latest=false` (adjust tag), then ensure the newest app tag is Latest (edit in the GitHub UI or recreate the app release with `--latest=true`).

**GitHub constraints**

- GitHub cannot host the **~2.6 GB** model as a **single** release asset.
- Split the model into **chunks under 2 GB** each.
- Use **SHA-256** verification for the APK, each chunk, and the **reconstructed** model file.

---

## 7. Safety and security rules

**Never commit**

- `local-models/`
- `dist/`
- `*.litertlm`
- `*.task`
- `*.jks`, `*.keystore`
- `signing.properties`
- `release-secrets/`
- GitHub tokens, API keys, or other secrets
- Model binaries
- Generated APK/AAB artifacts **unless** the maintainer explicitly intends them in a controlled release process (default: **do not** commit build outputs)

**Do not** embed GitHub tokens (or other secrets) in the app.

**App updates**

- **User-initiated** only.
- **Verify** APK **SHA-256** before install.
- Open the **Android installer** prompt; **never** silently install.

**Model downloads**

- **User-initiated** only.
- Download into **app-private** storage (not public Downloads as the runtime path).
- Verify **every chunk** SHA and the **full reconstructed** model SHA.
- **Do not** require `MANAGE_EXTERNAL_STORAGE`.

**Errors in UI**

- Normal UI: **no** raw stack traces or scary low-level errors.
- Developer diagnostics may show technical detail—keep that behind **developer-only** surfaces.

---

## 8. Build and test commands

Use **Android Studio’s JBR** for `JAVA_HOME` (example on Windows):

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
```

| Goal | Command |
|------|---------|
| Debug build | `.\gradlew.bat :app:assembleDebug` |
| Unit tests | `.\gradlew.bat :app:testDebugUnitTest` |
| Instrumented (device) tests | `.\gradlew.bat :app:connectedDebugAndroidTest` |
| Compile Kotlin (debug) | `.\gradlew.bat :app:compileDebugKotlin` |
| Release APK (signed) | `.\scripts\build-release-apk.ps1` after signing env vars are configured per maintainer docs |
| Local Pixel bundle (no zip) | `.\scripts\package-sarathi-pixel-bundle.ps1 -SkipZip` |
| GitHub release package | `.\scripts\package-github-release.ps1 -ReleaseType APP_ONLY` (or `FULL_MODEL`, `MODEL_ONLY`) |

**Primary physical QA device (documented)**

- **Pixel 10 Pro XL**  
- Serial: `57171FDCQ00AZ7`

**ADB fallback (if not on PATH)**

- `C:\Users\pruthvi\AppData\Local\Android\Sdk\platform-tools\adb.exe`

---

## 9. Pixel QA expectations

Meaningful **Gemma / LiteRT** validation should be done on a **physical Pixel**, not emulator-only.

**Why emulator is insufficient alone**

- Storage pressure.
- Public Downloads paths can trigger LiteRT **permission denied**.
- GPU / backend behavior may differ from production hardware.

**Physical Pixel checklist**

- App installs and launches.
- Model present at `files/models/gemma-4-E2B-it.litertlm`.
- Byte size **2 588 147 712**.
- Settings indicates on-device Gemma readiness (wording may evolve—verify the **ready** state for LiteRT).
- **Practice mode OFF** exercises **LiteRT-LM** path.
- Logcat shows: `LiteRT-LM load ok`, `LiteRT-LM generation start`, `LiteRT-LM generation end`.
- **Practice mode** still works when toggled.
- **RAG** retrieval still behaves sensibly.

**Useful log filter (Windows)**

```text
adb logcat -d | findstr /i "LiteRtLmGemmaChatEngine"
```

---

## 10. UX rules for future agents

When changing UI:

- Preserve **calm, sacred** tone; avoid technical clutter in primary surfaces.
- Keep **developer diagnostics** behind an **expandable** or settings-gated area.
- Keep text **readable**; respect **loading** states; avoid **duplicate sends**.
- Do not push the chat experience toward a **generic AI assistant**.

**Regression-style prompts for chat quality**

- “I worked hard but failed. What does the Gita say?”
- “I am afraid of the future.”
- “Tell me about dharma.”
- “Teach me one practical lesson from the Gita.”

---

## 11. Common pitfalls

- **Clearing app data** after copying the model **deletes** app-private files—including the model.
- **Do not** point the **selected runtime model path** at `/sdcard/Download` (or similar public storage).
- **Do not** accidentally ship a **release** APK signed with **debug** credentials.
- **Do not** assume **APP_ONLY** releases include model chunks—they usually do not.
- **Fresh installs** need either **FULL_MODEL** packaging flow or access to an external **MODEL_ONLY** manifest + chunks.
- **App-only** updates should **preserve** an already-installed compatible Gemma model.
- Changing **package name** or **signing key** breaks update continuity for existing users.
- If the **model SHA** changes, treat it as a **model** update / compatibility event.
- If `requiresModelUpdate` (or equivalent) is true and the installed model is **incompatible**, **block** LiteRT and **fall back** to practice mode **without** crashing.

---

## 12. Agent operating instructions

**Before changing code**

1. Read **AGENTS.md** (this file).
2. Check **`git status`**.
3. Classify the task: **UI**, **LLM runtime**, **RAG**, **release/update**, **model download**, **tests/docs**, or a narrow combination.
4. Avoid **broad refactors** unless explicitly requested.
5. Make **targeted** changes aligned with existing patterns.
6. Preserve **practice mode**, **LiteRT**, **RAG**, and **release scripts** unless the task explicitly changes them.
7. Run the **relevant** Gradle build and/or tests.
8. Update **docs** or internal reports when **behavior** or **release contracts** change.
9. **Never** claim Gemma works on device unless logs show **LiteRT-LM load** and **generation** success.
10. **Never** claim a GitHub release “works” unless assets are **actually published** and **smoke-tested** as appropriate.

---

## 13. File maintenance

If this project changes significantly (architecture, runtimes, release URLs, corpus layout, or safety rules), **update AGENTS.md in the same PR or commit** so future agents stay aligned with reality.

---

*End of AGENTS.md*
