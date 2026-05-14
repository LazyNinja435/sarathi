#Requires -Version 5.1
<#
.SYNOPSIS
  Builds a signed release APK and copies it to dist/github-release/sarathi-v<version>.apk
#>
$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$PropsPath = Join-Path $RepoRoot "sarathi-version.properties"

function Read-VersionName([string]$path) {
    $name = $null
    Get-Content $path | ForEach-Object {
        if ($_ -match '^\s*SARATHI_VERSION_NAME\s*=\s*(.+)\s*$') { $name = $Matches[1].Trim() }
    }
    if ([string]::IsNullOrWhiteSpace($name)) { throw "Could not read SARATHI_VERSION_NAME from $path" }
    return $name
}

$required = @(
    "SARATHI_KEYSTORE_PATH",
    "SARATHI_KEYSTORE_PASSWORD",
    "SARATHI_KEY_ALIAS",
    "SARATHI_KEY_PASSWORD"
)
foreach ($k in $required) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($k))) {
        throw "Missing environment variable: $k"
    }
}

$javaHome = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path $javaHome) { $env:JAVA_HOME = $javaHome }

$versionName = Read-VersionName $PropsPath
$OutDir = Join-Path $RepoRoot "dist\github-release"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$DestApk = Join-Path $OutDir ("sarathi-v{0}.apk" -f $versionName)

Push-Location $RepoRoot
try {
    & (Join-Path $RepoRoot "gradlew.bat") ":app:assembleRelease" --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "Gradle assembleRelease failed." }
}
finally {
    Pop-Location
}

$Built = Join-Path $RepoRoot "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $Built)) { throw "Release APK not found at $Built" }
Copy-Item $Built $DestApk -Force
Write-Host "Release APK copied to $DestApk"
