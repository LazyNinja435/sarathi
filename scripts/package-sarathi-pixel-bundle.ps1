#Requires -Version 5.1
<#
.SYNOPSIS
  Builds debug APK and assembles dist/sarathi-pixel-bundle with APK, model, install script, README, and optional zip.
.PARAMETER SkipZip
  When set, skips creating dist/sarathi-pixel-bundle.zip
#>
param(
    [switch] $SkipZip
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$Dist = Join-Path $RepoRoot "dist\sarathi-pixel-bundle"
$ModelSrc = Join-Path $RepoRoot "local-models\gemma4-e2b\gemma-4-E2B-it.litertlm"
$ApkSrc = Join-Path $RepoRoot "app\build\outputs\apk\debug\app-debug.apk"
$InstallScriptSrc = Join-Path $RepoRoot "scripts\install-sarathi-pixel.ps1"

$javaHome = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path $javaHome) {
    $env:JAVA_HOME = $javaHome
}

Push-Location $RepoRoot
try {
    Write-Host "Building debug APK..."
    & (Join-Path $RepoRoot "gradlew.bat") ":app:assembleDebug" --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "Gradle assembleDebug failed." }
}
finally {
    Pop-Location
}

if (-not (Test-Path $ApkSrc)) { throw "APK not found at $ApkSrc" }
if (-not (Test-Path $ModelSrc)) {
    Write-Warning "Model not found at $ModelSrc - copy gemma-4-E2B-it.litertlm there, then re-run packaging."
}

if (Test-Path $Dist) { Remove-Item -Recurse -Force $Dist }
New-Item -ItemType Directory -Path $Dist | Out-Null
New-Item -ItemType Directory -Path (Join-Path $Dist "logs") | Out-Null

Copy-Item $ApkSrc (Join-Path $Dist "app-debug.apk") -Force
if (Test-Path $ModelSrc) {
    Copy-Item $ModelSrc (Join-Path $Dist "gemma-4-E2B-it.litertlm") -Force
}
Copy-Item $InstallScriptSrc (Join-Path $Dist "install-sarathi-pixel.ps1") -Force

$readme = @"
# Sarathi Pixel Bundle

Repeatable install for **Sarathi** (com.sarathi.app) with the Gemma LiteRT-LM checkpoint in app-private storage.

## Contents

- app-debug.apk - debug build (debuggable for run-as).
- gemma-4-E2B-it.litertlm - on-device model (not committed to git).
- install-sarathi-pixel.ps1 - installs APK, streams model to files/models/, verifies size, launches app, captures logcat.
- logs/ - install logcat output.

## Install (from this folder)

    powershell -ExecutionPolicy Bypass -File .\install-sarathi-pixel.ps1

Or pass a device serial:

    powershell -ExecutionPolicy Bypass -File .\install-sarathi-pixel.ps1 -DeviceSerial YOUR_SERIAL

Prerequisites: USB debugging, adb (ANDROID_HOME or default SDK), model file next to the script.

Expected model size on device: 2588147712 bytes.

## Runtime path on device

run-as com.sarathi.app -> files/models/gemma-4-E2B-it.litertlm

Public Downloads is not used as the primary path.
"@

Set-Content -Path (Join-Path $Dist "README.md") -Value $readme -Encoding UTF8

Write-Host "Bundle created at: $Dist"

if (-not $SkipZip) {
    $zip = Join-Path $RepoRoot "dist\sarathi-pixel-bundle.zip"
    if (Test-Path $zip) { Remove-Item -Force $zip }
    Compress-Archive -Path (Join-Path $Dist "*") -DestinationPath $zip -Force
    Write-Host "Zip: $zip"
}
