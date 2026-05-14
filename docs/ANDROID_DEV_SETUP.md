# Android development setup (Sarathi, Windows)

## JDK / JAVA_HOME

Use Android Studio’s bundled JBR (JDK 17):

`C:\Program Files\Android\Android Studio\jbr`

PowerShell example for a one-off Gradle command:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug
```

## Android SDK and adb

- `local.properties` should set `sdk.dir` (Android Studio creates this).
- If `adb` is not on PATH, use the platform-tools binary directly, for example:

`C:\Users\pruthvi\AppData\Local\Android\Sdk\platform-tools\adb.exe`

## Emulator

**Pixel_9_Pro API 36** was used for smoke testing on this project (see integration report).

## Windows Gradle notes

- `gradle.properties` sets **`kotlin.incremental=false`** as a reliability workaround for occasional flaky Windows incremental/KSP issues.
- A **`mergeDebugResources`** / long-path style failure has been observed after some **`clean`** runs. Prefer **`.\gradlew.bat :app:assembleDebug`** without `clean` unless you need a full reset. If the daemon is wedged: `.\gradlew.bat --stop`, then delete **`app\build`** only if necessary, and retry `assembleDebug`.

## RAG asset rebuild

From repo root (after Python venv with `tools/rag-builder/requirements.txt`):

```powershell
py -m venv .venv
.\.venv\Scripts\activate
pip install -r tools\rag-builder\requirements.txt
python tools\rag-builder\scripts\build_all.py --download --normalize --index
```

Indexed DB and manifest are copied to `app\src\main\assets\rag\` by the indexer.
