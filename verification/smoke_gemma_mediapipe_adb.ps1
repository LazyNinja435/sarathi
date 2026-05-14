# Sarathi — Gemma / MediaPipe smoke helpers (Windows PowerShell)
# Requires: emulator or device, adb in SDK platform-tools.

$ErrorActionPreference = "Stop"
$adb = "C:\Users\pruthvi\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { throw "adb not found at $adb" }

$RepoRoot = Split-Path -Parent $PSScriptRoot
$CapDir = Join-Path $RepoRoot "verification\screenshots\gemma"
New-Item -ItemType Directory -Force -Path $CapDir | Out-Null

function Invoke-Adb { & $adb @args }

function Save-Screencap([string]$FileName) {
    $dest = Join-Path $CapDir $FileName
    Invoke-Adb shell screencap -p /sdcard/gemma_tmp.png
    Invoke-Adb pull /sdcard/gemma_tmp.png $dest | Out-Null
    Write-Host "Saved $dest"
}

Write-Host "Devices:"
Invoke-Adb devices

Write-Host "Install debug (from repo root):"
Write-Host '  $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:installDebug'

Write-Host "Launch app:"
Invoke-Adb shell monkey -p com.sarathi.app -c android.intent.category.LAUNCHER 1

# Optional: uncomment to grab a quick screenshot after manual navigation
# Start-Sleep -Seconds 2
# Save-Screencap "manual_01.png"

Write-Host "Done. Complete Settings/Chat steps manually or extend this script with uiautomator taps."
