# GitHub release bootstrap plan

This document describes how to ship Sarathi so **fresh installs** can download the offline Gemma model even when the **latest app release is APP_ONLY** (no model chunks in `sarathi-latest.json`). The app resolves chunk metadata from a **stable model-only manifest** (`model-latest.json`) published under a dedicated tag (for example `model-gemma-4-e2b`).

## URLs (defaults)

| Role | URL |
|------|-----|
| App manifest (latest) | `https://github.com/LazyNinja435/sarathi/releases/latest/download/sarathi-latest.json` |
| Model manifest (stable tag) | `https://github.com/LazyNinja435/sarathi/releases/download/model-gemma-4-e2b/model-latest.json` |

APP_ONLY `sarathi-latest.json` includes `modelSource.externalManifestUrl` pointing at the stable model manifest (and the app falls back to the same default URL when the field is absent on older manifests).

## Phase A: Publish stable model release

1. Place the monolithic model at `local-models/gemma4-e2b/gemma-4-E2B-it.litertlm` (gitignored).
2. Package: `.\android\scripts\package-github-release.ps1 -ReleaseType MODEL_ONLY`
3. Publish: `.\android\scripts\publish-github-release.ps1 -ReleaseType MODEL_ONLY -Tag model-gemma-4-e2b` (add `-ClobberUpload` only when replacing assets on an existing tag). New creates pass **`--latest=false`** so this release does not become the repository default “Latest” over the app line.
4. Verify in a browser or with `curl`:
   - `model-latest.json` downloads from the tag URL above.
   - Each `model.chunks[*].fileName` asset downloads from the same tag.

## Phase B: First app release v0.1.0 (APP_ONLY)

1. Package: `.\android\scripts\package-github-release.ps1 -VersionName 0.1.0 -VersionCode 1 -ReleaseType APP_ONLY`
2. Confirm `dist/github-release/sarathi-latest.json` has **empty** `model.chunks`, includes `modelSource.externalManifestUrl`, and **no** `*.part*` files.
3. Publish the app tag (for example `v0.1.0`) with `.\android\scripts\publish-github-release.ps1 -VersionName 0.1.0 -VersionCode 1 -ReleaseType APP_ONLY -Tag v0.1.0` (after configuring signing for the packaging step). New creates pass **`--latest=true`** so `releases/latest/download/sarathi-latest.json` tracks this APK release.
4. On a device: install the APK, open **Settings → Download offline model**, confirm chunks download from the **model** release tag and Gemma runs with practice mode off.

## Phase C: App-only update v0.1.1

1. Ship another **APP_ONLY** release with a higher `versionCode`.
2. On a device that already installed the model from Phase A: install the new APK (or use in-app update when enabled).
3. Expected: `files/models/gemma-4-E2B-it.litertlm` and `installed-model.json` remain; **no** multi-GB re-download unless the user explicitly requests a model update that targets a new catalog.

## Phase D: Future full-model or model-catalog change

- Use **FULL_MODEL** only when the APK release should embed chunk metadata on `sarathi-latest.json` (large GitHub upload with the app tag), **or** publish a **new model-only tag** and point `modelSource.externalManifestUrl` / default URL strategy at it.
- Set `app.requiresModelUpdate` to **true** when the installed model is incompatible and users must refresh before on-device Gemma is allowed.

## Operational notes

- Never commit `*.litertlm`, `*.apk`, keystores, or anything under `dist/`.
- Run `.\gradlew.bat :app:compileDebugKotlin` and `.\gradlew.bat :app:testDebugUnitTest` before tagging releases.
