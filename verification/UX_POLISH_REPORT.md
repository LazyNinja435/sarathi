# UX polish report — Sarathi (Pixel + Gemma readiness)

**Date:** 2026-05-14  

## UX changes made

- **Chat:** Loading copy set to **“The charioteer is reflecting…”**; input placeholder **“What rests upon your heart?”**; **send** visually de-emphasized and **blocked while generating**; **duplicate sends prevented** via `Mutex.tryLock` in `ChatViewModel`; **auto-scroll** to newest message / typing row; **IME + navigation bar** padding so the composer stays visible with keyboard; **suggestion chips** only before the first user message; **drawer** tinted to midnight indigo with gold nav labels.  
- **Runtime badge:** `OfflineBadge` now reflects **Practice mode**, **On-device wisdom**, or **Offline guidance** with user-friendly subtitles (avoids prominent “Mock” wording on the main chat surface).  
- **Message layout:** Wider assistant bubbles (~92% of screen width, cap 560 dp) with improved **line height** for long Gemma replies.  
- **Typography:** `bodyLarge` base size **17 sp** / **26 sp** line height for readability.  
- **Onboarding:** **Name** screen uses **`imePadding`** to reduce keyboard overlap.  
- **Verse of the Day:** Heading hierarchy adjusted (“Verse of the Day” primary, “Today’s whisper” secondary).  
- **Settings:** Split into **calm defaults** (offline mode, guidance engine, model status, tone, check model, reset onboarding, about) vs **collapsible Developer diagnostics** (technical runtime, paths, RAG status, last inference error, practice toggle, path hints). **Reset onboarding** confirms, clears onboarding keys via `UserPreferencesRepository.resetOnboarding()`, then navigates back to splash via `NavHost` (without `pm clear`, preserving models).  
- **Diagnostics plumbing:** `LlmLastErrorStore` records inference failures (LiteRT / MediaPipe fallback paths); cleared on successful generations. `SettingsViewModel` refreshes **RAG warm-up** and exposes **ragReady** for diagnostics.

## Screens updated

Splash, Name, Chat, Verse of the Day, Settings (major), plus shared components: `OfflineBadge`, `MessageBubble`, `SacredTextField`, `Type`.

## Build result

**PASS** — `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:connectedDebugAndroidTest` on Pixel 10 Pro XL (serial `57171FDCQ00AZ7`).

## Pixel install result

**PASS** — streamed install of `app-debug.apk` to the physical Pixel with `adb install -r`.

## Gemma test result

**Not re-executed end-to-end in this automation slice** (no second 2.5 GB push). Your prior baseline on the same hardware already showed LiteRT-LM load and generation. After pulling this build, run **TC-004–TC-006** from `verification/test-cases/SARATHI_PIXEL_UX_AND_GEMMA_TEST_CASES.md` once to re-confirm.

## Practice mode result

**PASS by construction** — practice toggle remains in developer diagnostics; `MockKrishnaChatEngine` path unchanged; new unit test covers stable fallback text.

## RAG result

**PASS** — `SettingsViewModel.refreshModelStatus()` warms RAG on IO; verse path unchanged; no edits under `knowledge/`.

## Screenshot / log paths

| Artifact | Path |
|----------|------|
| Screencap (automated single frame) | `verification/screenshots/pixel_ux_gemma/01_splash.png` (copy of `01_launch.png`) |
| Tail logcat | `verification/pixel_ux_gemma_logcat.txt` |
| Pixel bundle | `dist/sarathi-pixel-bundle/` (gitignored; contains `app-debug.apk`, install script, README; model when `local-models/…` present) |

Screens **02–12** from the full UX matrix were **not** captured automatically (would require scripted navigation or manual pass); use the test-case doc as a checklist.

## Known issues

- **Multi-user adb** on some OEM builds complicates `pm list` without `--user 0` (see install report).  
- **First `run-as` after install** can fail until the package is visible to the shell user; reinstall + retry resolves in typical setups.

## Next recommendations

- Run `dist\sarathi-pixel-bundle\install-sarathi-pixel.ps1` once per machine or after `pm clear` to repopulate `files/models/`.  
- Add optional **Compose UI smoke** (onboarding + Settings open) if CI picks up an emulator with GPU headroom.  
- Capture the remaining **02–12** screenshots during the next manual UX pass.
