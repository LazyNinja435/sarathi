# Release security checklist (Sarathi GitHub distribution)

Use this before tagging or publishing a release.

## Authentication and secrets

- [ ] **No GitHub token** is embedded in Android source, `BuildConfig`, or string resources.
- [ ] **No keystore** (`*.jks`, `*.keystore`) is committed.
- [ ] **No signing passwords** are committed (`signing.properties`, scripts with hard-coded secrets).
- [ ] **No model binaries** (`.litertlm`, `.task`) are committed.
- [ ] **No release bundles** (`dist/`, pixel bundle outputs) are committed.
- [ ] **CI secrets** (if used) live only in GitHub Actions secrets, not in the repo.

## Permissions and storage

- [ ] **No `MANAGE_EXTERNAL_STORAGE`** permission is requested.
- [ ] **No public Downloads directory** is used as the **runtime** path for the Gemma model installed via in-app download (chunks assemble under `filesDir/models/`).
- [ ] **HTTPS only** for manifest, APK, and chunk URLs (enforced in `GithubReleaseClient`).

## Update and install behavior

- [ ] **App updates require explicit user taps** (“Check for updates”, “Download update”, “Install update”). No silent installs.
- [ ] **Model download requires an explicit user tap** (“Download offline model”) after an informational dialog.
- [ ] **APK SHA-256** from `sarathi-latest.json` is verified before install proceeds.
- [ ] **Each model chunk SHA-256** is verified after download.
- [ ] **Full reconstructed model SHA-256** is verified before atomic rename to the final filename.
- [ ] **Android package installer** is used (`ACTION_VIEW` + `FileProvider` URI), so the OS prompts the user.
- [ ] **Unknown-app install flow** is documented for users who have not allowed installs from this source (`MANAGE_UNKNOWN_APP_SOURCES` intent when `canRequestPackageInstalls()` is false).

## Product regressions (spot checks)

- [ ] **RAG** behavior unchanged for normal chat flows.
- [ ] **Practice / mock mode** still works when enabled.
- [ ] **LiteRT-LM** still resolves models under `filesDir/models/` (and existing adb / Pixel bundle flows still valid).

## Residual / operational risks

- **Supply chain**: Users must trust the GitHub org, release tags, and TLS to GitHub CDNs.
- **Large downloads**: Multi-gigabyte model chunks can fail on flaky networks; the app deletes bad data on checksum failure but cannot guarantee resume across kills without future resume support.
- **Manifest integrity**: If an attacker controls the manifest host or DNS, they could point to a malicious APK. Mitigation is GitHub + HTTPS + user habit of verifying org/repo; optional future pinning is not implemented here.
