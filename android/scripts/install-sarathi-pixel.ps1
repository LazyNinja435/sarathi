#Requires -Version 5.1
<#
.SYNOPSIS
  Installs Sarathi debug APK and streams Gemma LiteRT-LM model into app-private storage on a physical Pixel (or selected device).

.PARAMETER DeviceSerial
  Optional adb serial. When omitted, prefers a physical device whose model name contains "Pixel", excluding emulators.
#>
param(
    [string] $DeviceSerial = ""
)

$ErrorActionPreference = "Stop"
$ExpectedModelBytes = 2588147712L
$BundleRoot = $PSScriptRoot
$ApkPath = Join-Path $BundleRoot "app-debug.apk"
$ModelPath = Join-Path $BundleRoot "gemma-4-E2B-it.litertlm"
$LogDir = Join-Path $BundleRoot "logs"
$LogFile = Join-Path $LogDir "pixel-install-logcat.txt"

function Find-Adb {
    $fromEnv = $env:ANDROID_HOME
    if ($fromEnv) {
        $cand = Join-Path $fromEnv "platform-tools\adb.exe"
        if (Test-Path $cand) { return (Resolve-Path $cand).Path }
    }
    $defaultSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $defaultSdk) { return (Resolve-Path $defaultSdk).Path }
    $pathAdb = (Get-Command adb -ErrorAction SilentlyContinue)
    if ($pathAdb) { return $pathAdb.Source }
    throw "adb.exe not found. Set ANDROID_HOME or install Android SDK platform-tools."
}

function Get-Devices {
    param([string] $Adb)
    $raw = & $Adb devices -l 2>&1 | Out-String
    $lines = $raw -split "`r?`n" | Where-Object { $_ -match "`tdevice$" -or $_ -match "`tunauthorized$" }
    $out = @()
    foreach ($line in $lines) {
        if ($line -match "^(\S+)\s+device\s+(.*)$") {
            $serial = $Matches[1]
            $rest = $Matches[2]
            $model = ""
            if ($rest -match "model:(\S+)") { $model = $Matches[1] }
            $out += [pscustomobject]@{ Serial = $serial; Model = $model; Line = $line }
        }
    }
    return $out
}

function Select-Device {
    param(
        [string] $Adb,
        [string] $SerialOverride
    )
    $devices = Get-Devices -Adb $Adb
    if ($devices.Count -eq 0) { throw "No adb devices in 'device' state. Connect USB debugging." }
    if ($SerialOverride) {
        $m = $devices | Where-Object { $_.Serial -eq $SerialOverride }
        if (-not $m) { throw "Device serial not found: $SerialOverride" }
        return $m[0].Serial
    }
    $physical = $devices | Where-Object { $_.Serial -notmatch "^emulator-" }
    if ($physical.Count -eq 0) { throw "Only emulator devices found. Connect a physical device or pass -DeviceSerial." }
    $pixel = $physical | Where-Object { $_.Model -match "Pixel" }
    if ($pixel.Count -ge 1) { return $pixel[0].Serial }
    return $physical[0].Serial
}

$adb = Find-Adb
Write-Host "Using adb: $adb"

if (-not (Test-Path $ApkPath)) { throw "Missing APK: $ApkPath" }
if (-not (Test-Path $ModelPath)) { throw "Missing model: $ModelPath" }

$serial = Select-Device -Adb $adb -SerialOverride $DeviceSerial
Write-Host "Selected device serial: $serial"

$apkPass = $false
$modelCopyPass = $false
$sizePass = $false
$launchPass = $false

try {
    Write-Host "Installing APK..."
    & $adb -s $serial install -r $ApkPath
    if ($LASTEXITCODE -ne 0) { throw "adb install failed with exit $LASTEXITCODE" }
    $apkPass = $true
}
catch {
    Write-Warning $_.Exception.Message
}

try {
    Write-Host "Preparing app-private models directory..."
    & $adb -s $serial shell "run-as com.sarathi.app sh -c 'mkdir -p files/models && rm -f files/models/gemma-4-E2B-it.litertlm'"
    if ($LASTEXITCODE -ne 0) { throw "run-as mkdir failed (is the app debuggable / installed?)." }

    Write-Host "Streaming model (this may take several minutes)..."
    $modelFull = (Resolve-Path $ModelPath).Path
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $adb
    $psi.Arguments = "-s $serial exec-in run-as com.sarathi.app sh -c `"cat > files/models/gemma-4-E2B-it.litertlm`""
    $psi.RedirectStandardInput = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false
    $proc = New-Object System.Diagnostics.Process
    $proc.StartInfo = $psi
    [void]$proc.Start()
    $fs = [System.IO.File]::OpenRead($modelFull)
    $buf = New-Object byte[] 1048576
    try {
        while (($read = $fs.Read($buf, 0, $buf.Length)) -gt 0) {
            $proc.StandardInput.BaseStream.Write($buf, 0, $read)
            $proc.StandardInput.BaseStream.Flush()
        }
    }
    finally {
        $fs.Close()
        $proc.StandardInput.Close()
    }
    $proc.WaitForExit()
    if ($proc.ExitCode -ne 0) {
        $err = $proc.StandardError.ReadToEnd()
        throw "Model stream adb exit $($proc.ExitCode): $err"
    }
    $modelCopyPass = $true
}
catch {
    Write-Warning $_.Exception.Message
}

try {
    $sizeOut = & $adb -s $serial shell "run-as com.sarathi.app stat -c '%s' files/models/gemma-4-E2B-it.litertlm" 2>&1
    $sizeStr = ($sizeOut | Out-String).Trim()
    $sizeVal = [long]$sizeStr
    Write-Host "Model size on device: $sizeVal (expected $ExpectedModelBytes)"
    if ($sizeVal -eq $ExpectedModelBytes) { $sizePass = $true } else { throw "Model size mismatch." }
}
catch {
    Write-Warning $_.Exception.Message
}

try {
    & $adb -s $serial logcat -c | Out-Null
    & $adb -s $serial shell monkey -p com.sarathi.app -c android.intent.category.LAUNCHER 1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "monkey launch failed" }
    $launchPass = $true
    Start-Sleep -Seconds 2
    if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir | Out-Null }
    & $adb -s $serial logcat -d > $LogFile
    Write-Host "Logcat saved: $LogFile"
}
catch {
    Write-Warning $_.Exception.Message
}

Write-Host ""
Write-Host "======== RESULT ========"
Write-Host ("APK install:              " + ($(if ($apkPass) { "PASS" } else { "FAIL" })))
Write-Host ("Model copy:              " + ($(if ($modelCopyPass) { "PASS" } else { "FAIL" })))
Write-Host ("Model size verified:     " + ($(if ($sizePass) { "PASS" } else { "FAIL" })))
Write-Host ("Launch:                  " + ($(if ($launchPass) { "PASS" } else { "FAIL" })))
Write-Host "========================"

if (-not ($apkPass -and $modelCopyPass -and $sizePass -and $launchPass)) { exit 1 }
