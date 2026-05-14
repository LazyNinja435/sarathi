# GitHub release distribution (Sarathi)

Source repository: https://github.com/LazyNinja435/sarathi

This document describes how Sarathi is distributed **without** bundling the Gemma LiteRT-LM checkpoint in git, while still enabling:

- First install from a **GitHub Release APK**
- **Settings-based** checks for newer APKs (`sarathi-latest.json`)
- **Chunked** downloads of the `.litertlm` model into **app-private storage**

## Release strategy

1. Maintainer bumps `sarathi-version.properties`.
2. Maintainer runs `scripts/package-github-release.ps1` locally (requires the monolithic model under `local-models/` and signing env vars for the APK step).
3. Outputs land in `dist/github-release/` (git-ignored):
   - `sarathi-v<version>.apk`
   - `sarathi-latest.json` (manifest)
   - `gemma-4-E2B-it.litertlm.partNNN` chunks (< 2 GiB each; default 900 MiB)
   - `checksums.sha256`
   - `RELEASE_NOTES.md` (copied from `tools/release/RELEASE_NOTES.template.md`)
4. Maintainer publishes with `scripts/publish-github-release.ps1` using an authenticated `gh` CLI.

CI may additionally build a signed APK via `.github/workflows/build-release.yml`, but **model publishing stays local** due to size.

## `sarathi-latest.json` (latest manifest)

Canonical URL (latest pointer):

`https://github.com/LazyNinja435/sarathi/releases/latest/download/sarathi-latest.json`

The manifest contains:

- `app` — `versionCode`, `versionName`, APK filename, **SHA-256**, size, `packageName`
- `model` — canonical filename, total **SHA-256**, declared chunk size, per-chunk metadata (filename, size, SHA-256)
- `release` — `repo`, concrete `tag`, and a copy of the manifest URL for documentation

The Android app downloads this JSON over HTTPS, compares `versionCode` to `BuildConfig.VERSION_CODE`, and builds asset URLs as:

`https://github.com/<repo>/releases/download/<tag>/<fileName>`

No GitHub token is embedded in the app.

## First-time install (end user)

1. Open the GitHub Releases page for Sarathi.
2. Download `sarathi-v0.1.0.apk` (or the filename referenced in the manifest).
3. Android shows the **system install prompt**; confirm installation.

## Offline model (end user)

1. Open **Settings**.
2. If LiteRT-LM is missing, use **Download offline model** (see `docs/IN_APP_UPDATES.md` for UX details).
3. The app downloads each `.partNNN` sequentially, verifies SHA-256, concatenates to `files/models/gemma-4-E2B-it.litertlm`, verifies the full hash, then deletes temporary chunk files.

## Maintainer cross-links

- Signing: `docs/ANDROID_RELEASE_SIGNING.md`
- In-app flows & troubleshooting: `docs/IN_APP_UPDATES.md`
- Security checklist: `verification/RELEASE_SECURITY_CHECKLIST.md`
