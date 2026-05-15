# In-app updates (Sarathi)

Sarathi can check for newer APKs and can download the offline Gemma model **from GitHub Release assets**. Both flows are **explicit** (user taps buttons) and **never** auto-install.

## App update flow

1. **Settings → Update Sarathi** shows the current `versionName` / `versionCode` from `BuildConfig`.
2. **Check for updates** downloads `sarathi-latest.json` from the latest GitHub release.
3. If `manifest.app.versionCode` is greater than the installed app:
   - UI shows **“A new version is available.”**, remote `versionName`, and APK size.
   - User taps **Download update** → APK saved to `cacheDir/updates/sarathi-update.apk`.
   - After download, Sarathi verifies **SHA-256** and size against the manifest. On mismatch, the APK is deleted.
4. User taps **Install update**:
   - If `PackageManager.canRequestPackageInstalls()` is **false** (Android 8+), Sarathi opens **Settings → Install unknown apps** for this package so the user can allow updates from the browser / file manager source.
   - Otherwise Sarathi launches the **Android package installer** with a `FileProvider` `content://` URI. **Android always prompts**; Sarathi cannot bypass that prompt.

### Expected strings (user-facing)

- “Update Sarathi”
- “Check for updates”
- “Download update”
- “Install update”
- “Android will ask you to confirm installation.”

### Developer diagnostics (Settings → Developer diagnostics)

- Manifest URL used for update checks
- Last downloaded update APK SHA-256 (after a successful download)
- Last update error string (no raw stack traces in normal UI)

## Offline model download flow

1. When LiteRT-LM is not found, Settings offers **Download offline model** after explaining the state (“On-device wisdom is not installed yet.”).
2. A confirmation dialog warns (~**2.6 GB**), recommends **Wi‑Fi**, and asks the user to keep the app open.
3. **Manifest resolution:** the app loads `sarathi-latest.json` (from the cached update check or by fetching `releases/latest/download/sarathi-latest.json`). If `model.chunks` is empty, it loads **`modelSource.externalManifestUrl`** (or the built-in default pointing at tag **`model-gemma-4-e2b`**) to obtain `model-latest.json`, then downloads chunks from **`release.tag`** on that catalog (typically the model-only release, not the app tag).
4. The app downloads each manifest-listed chunk into `filesDir/model-downloads/`, verifies per-chunk SHA-256, concatenates to `filesDir/models/gemma-4-E2B-it.litertlm.tmp`, verifies the **full-model** SHA-256 and size (**2588147712** bytes for the current bundle), then **atomically renames** to `filesDir/models/gemma-4-E2B-it.litertlm`.
5. `InstalledModelInfo` metadata is written to `files/models/installed-model.json`.
6. Temporary chunk files are deleted after success.
7. `SettingsViewModel.refreshModelStatus()` runs so diagnostics show **Ready**.

Failures (network, storage, SHA mismatch) show **plain-language** errors; corrupt partial files are removed when verification fails.

## Unknown-app install permission (Android 8+)

If installation is blocked, enable **Allow from this source** for the app that delivered the APK (often Chrome or Files). Sarathi may deep-link into the per-app unknown-sources screen when install permission is missing.

## Troubleshooting

| Symptom | Likely cause | What to try |
| --- | --- | --- |
| “HTTP 404” on update check | Release or `sarathi-latest.json` asset missing | Verify GitHub Release assets and `latest` tag points at intended release |
| “APK checksum did not match” | Partial download / CDN glitch / tampered file | Delete and re-download; verify manifest integrity at source |
| Install button does nothing | Installer not exposed (package visibility) / permission | Grant installs for Sarathi + source app; retry |
| Model download fails near end | Low storage | Free ~6+ GB transient space; retry after cleanup |
| “Only HTTPS URLs are supported” | Redirect to non-HTTPS | Fix hosting; app rejects non-TLS |

## adb / Pixel bundle compatibility

Existing workflows that push `gemma-4-E2B-it.litertlm` into `files/models/` continue to work; the in-app downloader is an additional path, not a replacement.
