# Sarathi GitHub release — Pixel / GitHub QA test cases

Device (expected): **Pixel 10 Pro XL**  
Serial (reference): `57171FDCQ00AZ7`

> If a public GitHub release is not published yet, run **TC-GH-003** locally and exercise **TC-GH-006 / TC-GH-007** against a **local HTTPS manifest** (for example a temporary static file host) or wait until `v0.1.0` assets exist. Document any gap in the QA log.

## TC-GH-001 Repo safety

Steps:

1. `git status --short`
2. `git remote -v`
3. `git check-ignore -v local-models/gemma4-e2b/gemma-4-E2B-it.litertlm`
4. `git check-ignore -v dist/sarathi-pixel-bundle/gemma-4-E2B-it.litertlm`
5. `git check-ignore -v dist/github-release/gemma-4-E2B-it.litertlm.part001`

Expected:

- No `*.litertlm`, `*.apk`, keystores, or `dist/` artifacts are tracked.
- Remote documents `LazyNinja435/sarathi` (or notes the fix command).

## TC-GH-002 Release APK signing

Steps:

1. Create keystore locally (`scripts/create-release-keystore.ps1`) **once**.
2. Export signing env vars (`SARATHI_KEYSTORE_PATH`, passwords, alias).
3. Run `android/scripts/build-release-apk.ps1`.

Expected:

- `dist/github-release/sarathi-v0.1.0.apk` exists and installs on device (may replace debug signature).

## TC-GH-003 Manifest generation (local)

Steps:

1. Ensure `local-models/gemma4-e2b/gemma-4-E2B-it.litertlm` exists locally (not in git).
2. **APP_ONLY:** `.\android\scripts\package-github-release.ps1 -ReleaseType APP_ONLY` (optional: `-VersionName 0.1.1 -VersionCode 2`).
3. **FULL_MODEL:** `.\android\scripts\package-github-release.ps1 -ReleaseType FULL_MODEL`.
4. **MODEL_ONLY:** `.\android\scripts\package-github-release.ps1 -ReleaseType MODEL_ONLY` (output under `dist/github-model-release/`).

Expected:

- `dist/github-release/sarathi-latest.json` exists with `schemaVersion` **1**, `release.releaseType` matching the script, and `app.supportedModelIds` populated (APP_ONLY / FULL_MODEL).
- **APP_ONLY:** `model.chunks` is empty; `modelSource.externalManifestUrl` points at the stable model manifest; no `*.part*` files in `dist/github-release`.
- **FULL_MODEL:** chunk files `*.part001..003` exist and are each **< 2 GiB**.
- **MODEL_ONLY:** `dist/github-model-release/model-latest.json` plus chunk files; no APK in that folder. New tooling writes `release.releaseType` **`MODEL_ONLY`**; older published catalogs may still say **`FULL_MODEL`** — the Android parser accepts both for chunk download.
- `python tools/release/verify_release_manifest.py --dist-dir dist/github-release` (or `--dist-dir dist/github-model-release` after MODEL_ONLY) exits **0**.

## TC-GH-004 Publish release (GitHub)

Steps:

1. `gh auth login` (interactive, not automated here).
2. **APP_ONLY:** `.\android\scripts\publish-github-release.ps1 -ReleaseType APP_ONLY -ClobberUpload` (only if re-uploading assets).
3. **FULL_MODEL:** `.\android\scripts\publish-github-release.ps1 -ReleaseType FULL_MODEL -ClobberUpload`.
4. **MODEL_ONLY:** `.\android\scripts\publish-github-release.ps1 -ReleaseType MODEL_ONLY -Tag model-gemma-4-e2b -ClobberUpload` (tag must match the packaged model manifest).

Expected:

- Release tag (for example `v0.1.0`) exists on `LazyNinja435/sarathi` for app releases.
- **APP_ONLY publish:** assets include APK, `sarathi-latest.json`, `checksums.sha256`, `RELEASE_NOTES.md` — **no** model part files required.
- **FULL_MODEL publish:** assets include the above **plus** all model `.partNNN` chunks.
- **MODEL_ONLY publish:** assets include `model-latest.json`, `checksums.sha256`, and all `.partNNN` files (no APK).
- `https://github.com/LazyNinja435/sarathi/releases/latest/download/sarathi-latest.json` downloads over HTTPS.
- Release notes state clearly: APP_ONLY does not bundle chunks; FULL_MODEL notes that the app will ask before downloading the new model.

## TC-GH-005 First install from GitHub (device)

Steps:

1. Download the release APK in Chrome on the Pixel.
2. Open the file, accept the OS install prompt.

Expected:

- Android installer UI appears; after install, Sarathi launches.

## TC-GH-006 In-app app update check

Steps:

1. Open **Settings → Update Sarathi → Check for updates**.

Expected:

- Either “Sarathi is up to date.” or “A new version is available.” with plausible size text (no raw stack traces).

## TC-GH-007 In-app model download

Steps:

1. Remove `files/models/gemma-4-E2B-it.litertlm` if present (for example `adb shell run-as com.sarathi.app rm files/models/gemma-4-E2B-it.litertlm` as appropriate).
2. **Settings → Offline model → Download offline model** (confirm dialog).

Expected:

- Chunks download into app-private storage, assemble, verify SHA, final file at `files/models/gemma-4-E2B-it.litertlm`.
- Settings shows **Model status: Ready** and **Guidance engine: On-device Gemma** when practice mode is off.

## TC-GH-008 Real Gemma after GitHub model install

Steps:

1. Practice mode **OFF**.
2. Ask: “I worked hard but failed. What does the Gita say?”

Expected:

- LiteRT-LM loads, generation completes, no crash.

## TC-GH-009 Practice mode fallback

Steps:

1. Practice mode **ON**.
2. Ask: “Teach me one practical lesson from the Gita.”

Expected:

- Scripted / practice response path works without requiring Gemma.

## TC-GH-010 Update / checksum safety

Steps:

1. Locally edit `dist/github-release/sarathi-latest.json` to corrupt **one hex character** in `app.apkSha256` (or a chunk SHA) for a throwaway copy.
2. Point the app at that manifest (developer test build with alternate URL **not shipped**) **or** intercept via local test server.

Expected:

- Download is **blocked**; file deleted; user sees a **friendly** error string (not a stack trace).

## TC-GH-013 App-only update preserves model

Preconditions:

- Sarathi installed.
- Gemma model installed at `files/models/gemma-4-E2B-it.litertlm`.
- Installed model size is **2588147712** (reference size for the current bundle).

Steps:

1. Install newer **APP_ONLY** APK with the same package name and signing key as the installed build.
2. Launch Sarathi.
3. Open **Settings**.
4. Tap **Check model** (and optionally **Verify model** after metadata refresh).
5. Ask Gemma a prompt with **Practice mode OFF**.

Expected:

- App version reflects the newer APK.
- Model file still exists at `files/models/gemma-4-E2B-it.litertlm`.
- Model size remains **2588147712**.
- Settings shows **On-device wisdom: Ready** when the on-disk model matches supported IDs / manifest.
- LiteRT-LM loads and generation completes end-to-end.
- The large model was **not** re-downloaded (no multi‑GB transfer).

## TC-GH-014 App-only update does not show large model download

Steps:

1. Current model is installed and compatible with the manifest (`supportedModelIds`, size/SHA as applicable).
2. Check updates against an **APP_ONLY** manifest (newer `app.versionCode` only).

Expected:

- App update prompt appears when the remote app is newer.
- No **required** ~2.6 GB model download prompt for a compatible install.

## TC-GH-015 Full model update offers model download

Steps:

1. Current model is installed.
2. Manifest has `release.releaseType` **FULL_MODEL** and a **new** `model.sha256` (optional update path: `app.requiresModelUpdate` false).
3. Open **Settings**.

Expected:

- User can still download/install the newer APK from the update card.
- Model update is surfaced separately (e.g. **New offline model available** / **Download model update**).
- User must confirm before any large model download starts.

## TC-GH-016 Required model update blocks Gemma but not app

Steps:

1. Install an app build whose manifest sets `app.requiresModelUpdate` **true** for a newer model.
2. Keep the **old** model file installed (wrong SHA / wrong generation).

Expected:

- App launches; **Practice mode** still works.
- On-device Gemma path shows **model update required** (badge / Settings); chat does not crash.
- User can start an explicit **Download model update** after confirmation.
- No uncaught crash from LiteRT load.

## TC-GH-017 Uninstall removes model

Steps:

1. Uninstall Sarathi from the device.
2. Reinstall from a GitHub release APK.

Expected:

- Prior app-private model files are gone (fresh app data).
- App prompts to **Download offline model** (or equivalent) again.

## TC-GH-018 Fresh install with latest APP_ONLY release downloads model from external model manifest

Preconditions:

- Latest GitHub app release is **APP_ONLY** (`sarathi-latest.json` has empty `model.chunks` and `modelSource.externalManifestUrl` or relies on the app default).
- Stable model release **model-gemma-4-e2b** exists with `model-latest.json` and matching `*.partNNN` assets.
- App-private model file is missing (fresh install or cleared `files/models`).

Steps:

1. Install the APK from the latest GitHub app release.
2. Launch Sarathi.
3. Open **Settings**.
4. Tap **Download offline model** (confirm any prompt).

Expected:

- The app uses the app manifest, detects no inline chunks, fetches **model-latest.json** from the stable model release.
- Chunk files download, assemble, full SHA verifies, model is written under `files/models/`.
- **Model status: Ready**; with practice mode off, LiteRT-LM Gemma completes a prompt.

## TC-GH-019 App-only update preserves externally installed model

Preconditions:

- Offline model was installed from the **model-gemma-4-e2b** (or equivalent) model-only release.

Steps:

1. Install a newer **APP_ONLY** APK (same signing key / package) over the existing app.
2. Launch Sarathi.
3. Open **Settings** and confirm model status / optional **Check model** or **Verify model**.
4. With practice mode **OFF**, run a short Gemma prompt.

Expected:

- Model file remains in app-private storage; `installed-model.json` remains consistent.
- Gemma works end-to-end.
- No automatic multi-GB model re-download.

## TC-GH-020 Model-only release can be updated independently

Steps:

1. Publish a new **MODEL_ONLY** release (new tag or updated `model-latest.json` with a newer `model.version` / SHA) following `android/docs/GITHUB_RELEASE_BOOTSTRAP_PLAN.md`.
2. On device, use **Settings** flows that check the model catalog (e.g. optional model update affordance when manifest indicates a newer bundle).

Expected:

- Model updates are offered **separately** from APK update checks.
- Large downloads require **explicit** user confirmation before starting.

## Artifacts to capture

- Logcat: `verification/github_release_update_logcat.txt` (filter: `Sarathi`, `AndroidRuntime`, `OkHttp` if added later).
- Screenshots under `verification/screenshots/github_update/`:
  - `01_settings_update.png`
  - `02_update_available_or_latest.png`
  - `03_model_download_prompt.png`
  - `04_model_downloading.png`
  - `05_model_ready.png`
  - `06_gemma_after_model_install.png`
