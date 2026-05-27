# Sarathi — LiteRT-LM Gemma 4 smoke helpers (Windows PowerShell)
# Requires: emulator or device, adb in SDK platform-tools, local .litertlm at repo path.

$ErrorActionPreference = "Stop"
$adb = "C:\Users\pruthvi\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { throw "adb not found at $adb" }

$RepoRoot = Split-Path -Parent $PSScriptRoot | Split-Path -Parent
$CapDir = Join-Path $RepoRoot "android\verification\screenshots\litert_gemma4"
New-Item -ItemType Directory -Force -Path $CapDir | Out-Null

function Invoke-Adb { & $adb @args }

function Save-Screencap([string]$FileName) {
    $dest = Join-Path $CapDir $FileName
    Invoke-Adb shell screencap -p /sdcard/sarathi_litert_tmp.png
    Invoke-Adb pull /sdcard/sarathi_litert_tmp.png $dest | Out-Null
    Write-Host "Saved $dest"
}

Write-Host "Devices:"
Invoke-Adb devices

$localModel = Join-Path $RepoRoot "local-models\gemma4-e2b\gemma-4-E2B-it.litertlm"
if (Test-Path $localModel) {
    $len = (Get-Item $localModel).Length
    Write-Host "Local model: $localModel ($len bytes)"
} else {
    Write-Warning "Local model missing at $localModel — adjust path before adb push."
}

Write-Host "`nPrepare device dirs:"
Invoke-Adb shell mkdir -p /sdcard/Download/sarathi
Invoke-Adb shell rm -rf /data/local/tmp/llm
Invoke-Adb shell mkdir -p /data/local/tmp/llm

Write-Host @"

Large push (multi-GB) — run manually when ready:
  & `"$adb`" push `"$localModel`" /sdcard/Download/sarathi/gemma-4-E2B-it.litertlm
  & `"$adb`" push `"$localModel`" /data/local/tmp/llm/model.litertlm

Optional run-as (debuggable builds):
  & `"$adb`" shell run-as com.sarathi.app mkdir -p files/models
  & `"$adb`" shell run-as com.sarathi.app cp /sdcard/Download/sarathi/gemma-4-E2B-it.litertlm files/models/gemma-4-E2B-it.litertlm

"@

Write-Host "Install debug (from repo root):"
Write-Host '  $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:installDebug'

Write-Host "`nLaunch app:"
Invoke-Adb shell monkey -p com.sarathi.app -c android.intent.category.LAUNCHER 1

Write-Host @"

Manual QA checklist:
  1) Settings -> Check model (expect LiteRT-LM file detected when pushed)
  2) Turn mock mode OFF -> Chat
  3) Prompts: failure / fear / dharma (see TEST_REPORT)
  4) Turn mock ON -> confirm mock path still works

Logcat capture example:
  & `"$adb`" logcat -c
  # exercise app...
  & `"$adb`" logcat -d > android\verification\litert_gemma4_logcat.txt

"@

Write-Host "Optional screenshot after manual navigation (uncomment in script):"
Write-Host "# Start-Sleep -Seconds 2"
Write-Host "# Save-Screencap `"01_settings_litert_found.png`""

Write-Host "`nDone."
