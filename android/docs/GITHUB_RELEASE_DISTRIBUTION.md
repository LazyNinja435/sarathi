# GitHub release distribution (Sarathi)

Source repository: https://github.com/LazyNinja435/sarathi

This document describes how Sarathi is distributed **without** bundling the Gemma LiteRT-LM checkpoint in git, while still enabling:

- First install from a **GitHub Release APK**
- **Settings-based** checks for newer APKs (`sarathi-latest.json`)
- **Chunked** downloads of the `.litertlm` model into **app-private storage**

## Release strategy

1. Maintainer bumps `sarathi-version.properties` (or passes `-VersionName` / `-VersionCode` into the packaging script).
2. Maintainer runs `android/scripts/package-github-release.ps1` locally. This **always** needs the monolithic model under `local-models/gemma4-e2b/` so tooling can embed **model metadata** (size, full SHA, supported id) into `sarathi-latest.json` even for **APP_ONLY** (the APK release does **not** upload `.part*` chunks).
3. **APP_ONLY** outputs land in `dist/github-release/` (git-ignored):
   - `sarathi-v<version>.apk`
   - `sarathi-latest.json` — `model.chunks` **empty**; `modelSource.externalManifestUrl` points at the stable model manifest URL under tag **`model-gemma-4-e2b`**
   - `checksums.sha256`
   - `RELEASE_NOTES.md`
4. **FULL_MODEL** adds `gemma-4-E2B-it.litertlm.partNNN` chunks in the same folder and lists them inline in `sarathi-latest.json`.
5. **MODEL_ONLY** outputs land in `dist/github-model-release/`: `model-latest.json` (with `release.releaseType` **`MODEL_ONLY`** for new packages; older publishes may still say `FULL_MODEL` — the app accepts both), chunk files, `checksums.sha256`.
6. Maintainer publishes with `android/scripts/publish-github-release.ps1` using an authenticated `gh` CLI. App releases are created with **`--latest=true`**; model-only releases use **`--latest=false`** so GitHub’s default “Latest” release remains the **app** line.

CI may additionally build a signed APK via `.github/workflows/build-release.yml`, but **model publishing stays local** due to size.

## `sarathi-latest.json` (latest manifest)

Canonical URL (latest pointer):

`https://github.com/LazyNinja435/sarathi/releases/latest/download/sarathi-latest.json`

The manifest contains:

- `app` — `versionCode`, `versionName`, APK filename, **SHA-256**, size, `packageName`, `supportedModelIds`, `requiresModelUpdate`
- `model` — canonical filename, total **SHA-256**, declared chunk size; for **APP_ONLY**, `chunks` is **empty** and the app resolves chunk URLs from **`modelSource.externalManifestUrl`** (stable `model-latest.json` on tag `model-gemma-4-e2b`)
- `release` — `repo`, concrete `tag`, `releaseType`, manifest URL
- `modelSource` (APP_ONLY) — `externalManifestUrl` for the model catalog

The Android app downloads this JSON over HTTPS, compares `versionCode` to `BuildConfig.VERSION_CODE`, and builds asset URLs as:

`https://github.com/<repo>/releases/download/<tag>/<fileName>`

No GitHub token is embedded in the app.

## First-time install (end user)

1. Open the GitHub Releases page for Sarathi.
2. Download `sarathi-v0.1.0.apk` (or the filename referenced in the manifest).
3. Android shows the **system install prompt**; confirm installation.

## Offline model (end user)

1. Open **Settings**.
2. If LiteRT-LM is missing, use **Download offline model** (see `android/docs/IN_APP_UPDATES.md` for UX details).
3. The app downloads each `.partNNN` sequentially, verifies SHA-256, concatenates to `files/models/gemma-4-E2B-it.litertlm`, verifies the full hash, then deletes temporary chunk files.

## Maintainer cross-links

- Signing: `android/docs/ANDROID_RELEASE_SIGNING.md`
- In-app flows & troubleshooting: `android/docs/IN_APP_UPDATES.md`
- Security checklist: `verification/RELEASE_SECURITY_CHECKLIST.md`
