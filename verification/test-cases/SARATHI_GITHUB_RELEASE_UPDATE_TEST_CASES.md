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
3. Run `scripts/build-release-apk.ps1`.

Expected:

- `dist/github-release/sarathi-v0.1.0.apk` exists and installs on device (may replace debug signature).

## TC-GH-003 Manifest generation (local)

Steps:

1. Ensure `local-models/gemma4-e2b/gemma-4-E2B-it.litertlm` exists locally (not in git).
2. Run `scripts/package-github-release.ps1`.

Expected:

- `dist/github-release/sarathi-latest.json` exists.
- Chunk files `*.part001..003` exist and are each **< 2 GiB**.
- `python tools/release/verify_release_manifest.py --dist-dir dist/github-release` exits **0**.

## TC-GH-004 Publish release (GitHub)

Steps:

1. `gh auth login` (interactive, not automated here).
2. `.\scripts\publish-github-release.ps1 -ClobberUpload` (only if re-uploading assets).

Expected:

- Release tag (for example `v0.1.0`) exists on `LazyNinja435/sarathi`.
- Assets include APK, `sarathi-latest.json`, `checksums.sha256`, and all model parts.
- `https://github.com/LazyNinja435/sarathi/releases/latest/download/sarathi-latest.json` downloads over HTTPS.

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

## Artifacts to capture

- Logcat: `verification/github_release_update_logcat.txt` (filter: `Sarathi`, `AndroidRuntime`, `OkHttp` if added later).
- Screenshots under `verification/screenshots/github_update/`:
  - `01_settings_update.png`
  - `02_update_available_or_latest.png`
  - `03_model_download_prompt.png`
  - `04_model_downloading.png`
  - `05_model_ready.png`
  - `06_gemma_after_model_install.png`
