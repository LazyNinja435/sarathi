# Sarathi — Pixel UX and Gemma test cases

Manual cases are primary for on-device LLM quality. Automated checks supplement where cheap.

---

## TC-001 — Pixel install package

- **ID:** TC-001  
- **Title:** Pixel install package  
- **Preconditions:** Pixel 10 Pro XL connected; USB debugging enabled; `gemma-4-E2B-it.litertlm` present next to install script in bundle folder.  
- **Steps:** From `dist/sarathi-pixel-bundle`, run `.\install-sarathi-pixel.ps1` (optionally `-DeviceSerial <serial>`).  
- **Expected result:** APK installs; model streams to app-private `files/models/gemma-4-E2B-it.litertlm`; size equals **2588147712**; app launches; `logs/pixel-install-logcat.txt` written.  
- **Evidence to capture:** Console PASS lines; logcat file; `adb shell run-as com.sarathi.app stat -c '%s' files/models/gemma-4-E2B-it.litertlm`.

---

## TC-002 — First launch onboarding

- **ID:** TC-002  
- **Title:** First launch onboarding  
- **Preconditions:** Fresh install or after **Reset onboarding** (do not `pm clear` if you need to keep the model).  
- **Steps:** Launch app; complete splash, name, tone, blessing.  
- **Expected result:** Flow is clear; keyboard does not hide required fields; selected tone persists in Settings.  
- **Evidence to capture:** Short screen recording or screenshots per step.

---

## TC-003 — Settings detects Gemma

- **ID:** TC-003  
- **Title:** Settings detects Gemma  
- **Preconditions:** Model installed; practice mode OFF.  
- **Steps:** Open Settings; tap **Check model**.  
- **Expected result:** Model status **Ready**; guidance engine **On-device Gemma**; Developer diagnostics show **LiteRT-LM** and a path under app files.  
- **Evidence to capture:** Screenshot of Settings (normal + diagnostics).

---

## TC-004 — Real Gemma response

- **ID:** TC-004  
- **Title:** Real Gemma response  
- **Preconditions:** Practice mode OFF; model present.  
- **Steps:** Ask: *I worked hard but failed. What does the Gita say?*  
- **Expected result:** Logcat shows LiteRT-LM load/generation; reply is not identical canned mock; Krishna-inspired tone; no crash.  
- **Evidence to capture:** `adb logcat -s LiteRtLmGemmaChatEngine:I`; chat screenshot.

---

## TC-005 — Fear prompt

- **ID:** TC-005  
- **Title:** Fear prompt  
- **Preconditions:** Practice mode OFF; Gemma available.  
- **Steps:** Ask: *I am afraid of the future.*  
- **Expected result:** Generated response; comforting tone; no deterministic mock string unless LiteRT failed over to mock.  
- **Evidence to capture:** Chat screenshot; relevant log lines.

---

## TC-006 — Dharma prompt

- **ID:** TC-006  
- **Title:** Dharma prompt  
- **Preconditions:** Practice mode OFF; Gemma available.  
- **Steps:** Ask: *Tell me about dharma.*  
- **Expected result:** Gita-style guidance; long reply remains readable (scrolling, line spacing).  
- **Evidence to capture:** Chat screenshot.

---

## TC-007 — Practice mode fallback

- **ID:** TC-007  
- **Title:** Practice mode fallback  
- **Preconditions:** None.  
- **Steps:** Turn **Practice mode** ON in Developer diagnostics; ask: *Teach me one practical lesson from the Gita.*  
- **Expected result:** Scripted practice reply; no crash.  
- **Evidence to capture:** Screenshot of Settings + chat.

---

## TC-008 — RAG-backed verse screen

- **ID:** TC-008  
- **Title:** RAG-backed verse screen  
- **Preconditions:** Bundled RAG DB present (default app build).  
- **Steps:** Open **Verse of the Day**.  
- **Expected result:** Verse and reflection load; no RAG error toast or empty error state.  
- **Evidence to capture:** Screenshot.

---

## TC-009 — When I Feel

- **ID:** TC-009  
- **Title:** When I Feel  
- **Preconditions:** None.  
- **Steps:** Open **When I Feel**; choose **Afraid**; read guidance; use **Continue** if offered.  
- **Expected result:** Calming copy; optional navigation to chat with prefilled prompt if implemented.  
- **Evidence to capture:** Screenshot.

---

## TC-010 — My Dharma

- **ID:** TC-010  
- **Title:** My Dharma  
- **Preconditions:** None.  
- **Steps:** Open **My Dharma**; enter a duty being avoided; read guidance; optionally **Reflect with Krishna**.  
- **Expected result:** Readable layout; reflection or chat flow works.  
- **Evidence to capture:** Screenshot.

---

## TC-011 — Loading state

- **ID:** TC-011  
- **Title:** Loading state  
- **Preconditions:** Practice mode OFF; Gemma available.  
- **Steps:** Send a prompt that triggers generation.  
- **Expected result:** Assistant shows **The charioteer is reflecting…**; send control disabled while generating.  
- **Evidence to capture:** Screenshot during generation.

---

## TC-012 — No duplicate sends

- **ID:** TC-012  
- **Title:** No duplicate sends  
- **Preconditions:** Practice mode OFF; Gemma available.  
- **Steps:** Tap send repeatedly during generation.  
- **Expected result:** Single user turn; single model invocation (log generation start/end once per send).  
- **Evidence to capture:** Logcat around one user message.

---

## TC-013 — App relaunch persistence

- **ID:** TC-013  
- **Title:** App relaunch persistence  
- **Preconditions:** Onboarding completed; model installed via bundle.  
- **Steps:** Force-stop app; relaunch.  
- **Expected result:** Onboarding not repeated; model still detected; Settings unchanged for tone and paths.  
- **Evidence to capture:** Before/after Settings screenshots.

---

## TC-014 — Developer diagnostics

- **ID:** TC-014  
- **Title:** Developer diagnostics  
- **Preconditions:** None.  
- **Steps:** Open Settings; expand **Developer diagnostics**.  
- **Expected result:** Runtime, model path, file type, RAG status, last inference error visible; normal user not forced to read stack traces on main Settings surface.  
- **Evidence to capture:** Screenshot of diagnostics card.

---

## TC-015 — No model binary tracked

- **ID:** TC-015  
- **Title:** No model binary tracked  
- **Preconditions:** Git workspace clean except local artifacts.  
- **Steps:** `git status --short`; `git check-ignore -v path/to/model` and `git check-ignore -v dist/sarathi-pixel-bundle`.  
- **Expected result:** `*.litertlm`, `local-models/`, `dist/` ignored; no model in `git status`.  
- **Evidence to capture:** Terminal output.

---

## Automated tests (local JVM / device)

| Area | Test class | Notes |
|------|------------|--------|
| Prompt + RAG wiring | `PromptBuilderTest` | Asserts scripture context block in prompt when RAG results provided. |
| Mock stability | `MockKrishnaChatEngineTest` | Unknown query returns stable body; anxiety branch sanity. |
| Model resolution | `ModelManagerInstrumentedTest` | On device/emulator: `.litertlm` in `filesDir/models` preferred over `.task` when both exist. |

Run JVM tests: `gradlew :app:testDebugUnitTest`  
Run instrumented: `gradlew :app:connectedDebugAndroidTest` (set `ANDROID_SERIAL` to the physical Pixel when multiple devices are attached).
