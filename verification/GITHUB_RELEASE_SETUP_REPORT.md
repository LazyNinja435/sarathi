# GitHub release setup — engineering report

Date: 2026-05-14  
Repo: https://github.com/LazyNinja435/sarathi  
Workspace: `D:\MyProjects\Sarathi`

## 1. Repo safety

- `.gitignore` extended with `*.jks`, `signing.properties`, `release-secrets/`, `.github/tmp/`.
- `git check-ignore` confirms model, `dist/`, and chunk paths are ignored.
- **Git remote** was **not configured** in this clone at verification time — see `verification/GITHUB_REPO_SAFETY_REPORT.md` for `git remote add` instructions.
- Heuristic scan for `ghp_*` / `github_pat_*` tokens in `app/src`, `scripts`, and `tools`: **no hits**.

## 2. Signing

- `app/build.gradle.kts` reads `SARATHI_*` env vars; `release` uses them when present, otherwise falls back to **debug** signing so `assembleRelease` still compiles for contributors.
- Scripts: `scripts/create-release-keystore.ps1`, `scripts/print-release-cert-fingerprint.ps1`, `scripts/build-release-apk.ps1`.
- Documentation: `docs/ANDROID_RELEASE_SIGNING.md`.

## 3. Release packaging + chunking

- Tools: `tools/release/split_model.py`, `create_release_manifest.py`, `write_checksums.py`, `verify_release_manifest.py`.
- Orchestrator: `scripts/package-github-release.ps1` (calls build + split + manifest + checksums + verify).
- **Not executed here** against the real 2.5 GB model file (not present in the agent environment); logic matches the required **900 MiB** chunk size (`943718400` bytes).

## 4. GitHub publish

- Script: `scripts/publish-github-release.ps1` (`gh` CLI, optional `-SkipModel`, `-Draft`, `-Prerelease`, `-ClobberUpload`).
- **Not executed** (no authenticated `gh` session in this workspace). Follow script output after `gh auth login`.

## 5. In-app updater

- Package: `com.sarathi.app.update` (`ReleaseManifest`, `GithubReleaseClient`, `AppUpdateManager`, `UpdateDownloadManager`, `ApkInstaller`, `UpdateViewModel`, etc.).
- Settings card **Update Sarathi** wired with required strings and installer flow.
- `assembleDebug` **PASS** locally after integration.

## 6. In-app model downloader

- Package: `com.sarathi.app.modeldownload` (`ModelDownloadManager`, `ModelChunkDownloader`, `ModelInstallViewModel`, etc.).
- Uses `filesDir/model-downloads/` + `filesDir/models/` only.

## 7. Pixel QA

- Test cases authored: `verification/test-cases/SARATHI_GITHUB_RELEASE_UPDATE_TEST_CASES.md`.
- **Not re-run on hardware** in this session (no attached Pixel / no published release in this environment). Logcat placeholder: `verification/github_release_update_logcat.txt`. Screenshot folder guide: `verification/screenshots/github_update/README.txt`.

## 8. CI (optional)

- Workflow: `.github/workflows/build-release.yml` (Windows runner + `gradlew.bat` + `android-actions/setup-android` + signed `assembleRelease` + APK artifact).

## Open risks / next steps

1. Add `git remote` + push a `v0.1.0` tag after review.
2. Run `package-github-release.ps1` on a machine that holds the real Gemma file; publish with `publish-github-release.ps1`.
3. Execute Pixel test matrix TC-GH-001 … TC-GH-010 and attach screenshots + logcat.
4. Consider future enhancements: resumable chunk downloads, delta updates, manifest pinning.
