# Android release signing (Sarathi)

Sarathi release builds are signed with a **local keystore** that you create yourself. Signing credentials are supplied **only via environment variables** (or GitHub Actions secrets mapped into env vars). Nothing sensitive is committed to git.

## Why a stable signing key matters

Android treats an app upgrade as the **same app** only if the signing certificate matches. If you lose the keystore or change keys arbitrarily, users cannot upgrade in place; they must uninstall (losing local data) or you must publish under a new package name.

## One-time keystore creation (Windows)

From the repo root:

```powershell
.\scripts\create-release-keystore.ps1
```

This writes (ignored by git):

`release-secrets/sarathi-release.jks`

Default alias in the helper script: `sarathi` (set `SARATHI_KEY_ALIAS` accordingly).

## Local environment variables (PowerShell)

```powershell
$env:SARATHI_KEYSTORE_PATH="D:\MyProjects\Sarathi\release-secrets\sarathi-release.jks"
$env:SARATHI_KEYSTORE_PASSWORD="(keystore password)"
$env:SARATHI_KEY_ALIAS="sarathi"
$env:SARATHI_KEY_PASSWORD="(key password — may match keystore password)"
```

Then build a release APK:

```powershell
.\scripts\build-release-apk.ps1
```

Or Gradle directly:

```powershell
.\gradlew.bat :app:assembleRelease
```

When the four variables are set and the keystore file exists, Gradle configures the `release` signing config. If they are **missing**, `assembleRelease` still succeeds using the **debug** keystore for signing (convenient for contributors, **not** for public distribution).

### Certificate fingerprint (Play / device attestation notes)

```powershell
.\scripts\print-release-cert-fingerprint.ps1
```

## Version bumps

Centralized in `sarathi-version.properties`:

```properties
SARATHI_VERSION_CODE=1
SARATHI_VERSION_NAME=0.1.0
```

`app/build.gradle.kts` reads these values into `versionCode` / `versionName`.

## GitHub Actions (optional)

Workflow: `.github/workflows/build-release.yml`

Secrets:

| Secret | Purpose |
| --- | --- |
| `SARATHI_RELEASE_KEYSTORE_B64` | Base64 of the **entire** `.jks` file |
| `SARATHI_KEYSTORE_PASSWORD` | Keystore password |
| `SARATHI_KEY_ALIAS` | Key alias (for example `sarathi`) |
| `SARATHI_KEY_PASSWORD` | Key password |

The workflow decodes the keystore on the runner, exports `SARATHI_KEYSTORE_PATH` via `$GITHUB_ENV`, and runs `gradlew.bat :app:assembleRelease` on **Windows** (this repo currently ships `gradlew.bat`).

> The workflow does **not** upload Gemma model assets. Model chunking remains a **local maintainer** step because of size.

## Operational warnings

- **Back up** `sarathi-release.jks` and passwords in a real secret manager; GitHub loss + no backup = no upgrade path.
- Never commit `release-secrets/`, `*.jks`, `*.keystore`, or `signing.properties`.
