# GitHub release bootstrap report

Date: 2026-05-14
Workspace: `D:\MyProjects\Sarathi`

## Task 1 — MODEL_ONLY manifest (live)

Fetched
`https://github.com/LazyNinja435/sarathi/releases/download/model-gemma-4-e2b/model-latest.json`

Verified:

| Field | Value |
|--------|--------|
| `schemaVersion` | `1` |
| `release.tag` | `model-gemma-4-e2b` |
| `release.releaseType` | `FULL_MODEL` (legacy; new packages emit `MODEL_ONLY` via `create_release_manifest.py`) |
| `model.id` | `gemma-4-e2b-it-litertlm` |
| `model.fileName` | `gemma-4-E2B-it.litertlm` |
| `model.sizeBytes` | `2588147712` |
| `model.sha256` | present (hex) |
| `model.chunks` | **3** entries (`part001`–`part003`) with `fileName`, `sizeBytes`, `sha256` |

Chunk download URLs follow
`https://github.com/LazyNinja435/sarathi/releases/download/model-gemma-4-e2b/<fileName>`
(asset names match manifest).

## Task 2 — APP_ONLY packaging (local)

Command:

`.\android\scripts\package-github-release.ps1 -VersionName 0.1.0 -VersionCode 1 -ReleaseType APP_ONLY`

**Result:** **Blocked** in this environment — `android/scripts/build-release-apk.ps1` requires signing env vars (`SARATHI_KEYSTORE_PATH`, `SARATHI_KEYSTORE_PASSWORD`, `SARATHI_KEY_ALIAS`, `SARATHI_KEY_PASSWORD`).

`sarathi-version.properties` is set to **0.1.0 (1)** for the intended first app release.

**Maintainer:** export the signing variables, re-run packaging, then confirm `dist/github-release/` contains:

- `sarathi-v0.1.0.apk`
- `sarathi-latest.json` (empty `model.chunks`, `modelSource.externalManifestUrl` → stable model manifest)
- `checksums.sha256`
- `RELEASE_NOTES.md`

## Task 3 — Publish APP_ONLY

Command (after Task 2 succeeds):

`.\android\scripts\publish-github-release.ps1 -VersionName 0.1.0 -ReleaseType APP_ONLY -Tag v0.1.0`

`publish-github-release.ps1` now passes **`--latest=true`** on **new** app/full-model release creates, and **`--latest=false`** on **new** MODEL_ONLY creates.

**Current GitHub state (`gh release list`):** the only visible release is **Sarathi offline model** (`model-gemma-4-e2b`). It was incorrectly marked **Latest** (so GitHub’s “Latest” pointer did not favor a future app line).

**Action taken (this session):** ran
`gh release edit model-gemma-4-e2b --repo LazyNinja435/sarathi --latest=false`
so the model release is **not** explicitly promoted as Latest. (`GET …/releases/tags/model-gemma-4-e2b` → `make_latest` false.)

**Note:** `https://github.com/LazyNinja435/sarathi/releases/latest/download/sarathi-latest.json` returns **404** until an app release ships **`sarathi-latest.json`** as an asset and is marked Latest — the model release never carried that file anyway.

## Task 4 — In-app model download (code)

Already implemented and verified in code review + unit tests:

- `ModelDownloadManager.resolveManifestForModelDownload` — inline chunks else `modelSource.externalManifestUrl` / default URL → fetch `model-latest.json` without overwriting app manifest cache.
- `ModelChunkDownloader` — per-chunk SHA-256 and size check.
- `ModelDownloadManager.downloadAndInstall` — `filesDir/model-downloads/`, `.tmp` assemble, full SHA + size, rename, `InstalledModelInfo.persistAfterVerification`, clear temp chunks; friendly `IllegalStateException` messages for UI.

## Task 5 — App-only update preserves model

- Normal Android upgrade **does not** clear `filesDir`; Sarathi does not delete `files/models/` on APK update.
- `OnDeviceWisdomStatus` + Settings copy treat **APP_ONLY** manifests as “model stays installed.”
- Optional “new model” UX still keys off **FULL_MODEL** app manifests (and defensive **MODEL_ONLY** parsing for catalogs).

## Task 6 / 7 — Device QA

Not executed from this agent session (no ADB session to the Pixel). Log capture template:
`verification/github_first_install_model_download_logcat.txt`

**v0.1.1 draft:** package with
`.\android\scripts\package-github-release.ps1 -VersionName 0.1.1 -VersionCode 2 -ReleaseType APP_ONLY`
after signing is configured; publish with `-Draft` if desired.

## Task 8 — Docs / tests

- `tools/release/create_release_manifest.py` — **MODEL_ONLY** manifests now set `release.releaseType` to **`MODEL_ONLY`**.
- `ReleaseManifest.kt` — **`MODEL_ONLY`** enum + parsing (legacy **`FULL_MODEL`** string on model catalogs still supported).
- `SettingsScreen.kt` — update card handles **MODEL_ONLY** like **FULL_MODEL**; Ready copy mentions installed model; model-required title aligned with product wording.
- `android/scripts/publish-github-release.ps1` — **`--latest`** behavior for app vs model creates.
- Updated: `AGENTS.md`, `android/docs/GITHUB_RELEASE_DISTRIBUTION.md`, `android/docs/IN_APP_UPDATES.md`, `android/docs/GITHUB_RELEASE_BOOTSTRAP_PLAN.md`, `verification/test-cases/SARATHI_GITHUB_RELEASE_UPDATE_TEST_CASES.md`, this report.

## Automated build

| Command | Result |
|---------|--------|
| `.\gradlew.bat :app:testDebugUnitTest` (JAVA_HOME = Android Studio JBR) | **PASS** |

## Risks / follow-ups

- Until **`v0.1.0`** is published and marked **Latest**, the app’s default update URL may still serve **`sarathi-latest.json` from the wrong release** if that asset exists on the model release (unlikely). Prefer publishing **`v0.1.0`** with **`sarathi-latest.json`** only on the app tag, then fix **Latest** as above.
- Republishing **MODEL_ONLY** with the updated Python will emit **`MODEL_ONLY`** in JSON; GitHub live file can stay **`FULL_MODEL`** until the next model asset refresh.
