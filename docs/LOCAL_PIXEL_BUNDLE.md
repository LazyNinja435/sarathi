# Local Sarathi Pixel bundle

Repeatable **APK + Gemma LiteRT-LM** install without checking the large checkpoint into git.

## Create the bundle

From the repo root (Windows):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\scripts\package-sarathi-pixel-bundle.ps1
```

Place the model once at:

`local-models/gemma4-e2b/gemma-4-E2B-it.litertlm`

The script builds `:app:assembleDebug`, then writes:

`dist/sarathi-pixel-bundle/`

- `app-debug.apk`
- `gemma-4-E2B-it.litertlm` (copied when the source file exists)
- `install-sarathi-pixel.ps1`
- `README.md`
- `logs/` (populated when you run the installer)

Optional zip: omit `-SkipZip` on the packaging script to also create `dist/sarathi-pixel-bundle.zip`.

## Install on a physical Pixel

1. Enable **USB debugging** on the phone.  
2. Prefer a single physical device, or pass **`-DeviceSerial`** to the script.  
3. From inside the bundle folder:

```powershell
.\install-sarathi-pixel.ps1
```

The installer resolves **adb** in this order:

1. `%ANDROID_HOME%\platform-tools\adb.exe`  
2. `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`  
3. `adb` on `PATH`

## Why the model is not in the default APK

The Gemma checkpoint is about **2.5 GB**. Shipping it inside every debug or release APK would slow installs, inflate CI artifacts, and confuse users who sideload without that much storage. The bundle keeps the **APK lean** and copies the model **once** into **app-private** storage where Sarathi already looks first.

## Where the model lands on device

After a successful stream:

`files/models/gemma-4-E2B-it.litertlm`

as seen from `run-as com.sarathi.app` (debuggable builds only). This matches `ModelManager.LITERT_LM_FILENAMES` and avoids relying on public **Downloads** (often permission-hostile on current Android).

## Troubleshooting

| Symptom | What to check |
|--------|----------------|
| **No device found** | `adb devices -l`; authorize RSA prompt on phone; try another cable/port. |
| **Multiple devices** | Use `-DeviceSerial` or unplug emulator; script ignores `emulator-*` when choosing a Pixel. |
| **`run-as` / `exec-in` failed** | Install **debug** APK (`app-debug.apk`); release builds are not debuggable. Reinstall with `adb install -r`. |
| **No space left** | Free internal storage (~3 GB headroom recommended for copy + temp). |
| **Model size mismatch** | Expected bytes: **2588147712**. Delete partial file under `files/models/` and re-run installer. |
| **LiteRT fallback to mock** | Logcat `LiteRtLmGemmaChatEngine`; check last inference error in **Settings → Developer diagnostics**; confirm CPU backend and device compatibility. |
