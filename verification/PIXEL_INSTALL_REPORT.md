# Pixel install report — Sarathi QA

**Date:** 2026-05-14  
**Host:** Windows build agent / developer workstation  

## Selected device

| Field | Value |
|--------|--------|
| **Serial** | `57171FDCQ00AZ7` |
| **adb model (`ro.product.model`)** | Pixel 10 Pro XL |
| **Android release (`ro.build.version.release`)** | 16 |
| **API (`ro.build.version.sdk`)** | 36 |

**Note:** `adb` was invoked from `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` when not on `PATH`. An **emulator** (`emulator-5554`) was also attached; explicit `-s` / `ANDROID_SERIAL` targeted the physical Pixel for install and `connectedDebugAndroidTest`.

## Build

| Step | Result |
|------|--------|
| `gradlew :app:assembleDebug` | **PASS** |
| `gradlew :app:testDebugUnitTest` | **PASS** |
| `gradlew :app:connectedDebugAndroidTest` (Pixel serial set) | **PASS** (1 test) |

## APK install (direct adb)

| Step | Result |
|------|--------|
| `adb -s 57171FDCQ00AZ7 install -r app\build\outputs\apk\debug\app-debug.apk` | **PASS** (Streamed Install / Success) |

## Model copy (bundle / device)

| Step | Result |
|------|--------|
| Host bundle `dist\sarathi-pixel-bundle\gemma-4-E2B-it.litertlm` | **Present** (expected **2588147712** bytes when copied from `local-models\…`) |
| Device `run-as com.sarathi.app … files/models` after this smoke | **Not re-verified** in this run (no `pm clear`; optional full `install-sarathi-pixel.ps1` stream left to operator to avoid redundant 2.5 GB transfer) |

To verify on device after running the installer:

```text
adb -s <SERIAL> shell "run-as com.sarathi.app stat -c '%s' files/models/gemma-4-E2B-it.litertlm"
```

Expected: `2588147712`

## Launch

| Step | Result |
|------|--------|
| `adb shell monkey -p com.sarathi.app -c android.intent.category.LAUNCHER 1` | **PASS** |

## LiteRT log snippet

Tail logcat captured to `verification/pixel_ux_gemma_logcat.txt`. On a cold launch without a prompt, **LiteRtLmGemmaChatEngine** lines may be absent until first Gemma generation. Prior device verification (user baseline) already confirmed load/generation end logs.

## Known issues / notes

- **Work profile / multi-user:** Some `adb shell pm list packages` variants can throw `Shell does not have permission to access user …` when the default adb user is ambiguous; prefer **`--user 0`** or explicit `-s` + primary user workflows for scripting.  
- **`findstr` on Unicode logcat:** Windows `findstr` may warn on UTF-16 log files; use `Select-String` in PowerShell or `rg` if needed.
