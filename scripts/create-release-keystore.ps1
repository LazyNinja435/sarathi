#Requires -Version 5.1
<#
.SYNOPSIS
  Creates a local release keystore under release-secrets/ (never commit this folder).
#>
$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$SecretsDir = Join-Path $RepoRoot "release-secrets"
$Keystore = Join-Path $SecretsDir "sarathi-release.jks"

New-Item -ItemType Directory -Force -Path $SecretsDir | Out-Null
if (Test-Path $Keystore) {
    throw "Keystore already exists: $Keystore"
}

$keytool = "keytool"
try {
    & $keytool -help | Out-Null
} catch {
    throw "keytool not found on PATH. Install a JDK and ensure JAVA_HOME/bin is on PATH."
}

Write-Host "Creating keystore at $Keystore (RSA 2048, 10_000-day validity)..."
& $keytool -genkeypair -v `
    -storetype JKS `
    -keystore $Keystore `
    -alias sarathi `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -dname "CN=Sarathi Release, OU=Engineering, O=Sarathi, L=Local, ST=Local, C=US"

Write-Host ""
Write-Host "Next steps (PowerShell, current session):"
Write-Host ('  $env:SARATHI_KEYSTORE_PATH="' + $Keystore + '"')
Write-Host '  $env:SARATHI_KEYSTORE_PASSWORD="(your keystore password)"'
Write-Host '  $env:SARATHI_KEY_ALIAS="sarathi"'
Write-Host '  $env:SARATHI_KEY_PASSWORD="(your key password — can match keystore password)"'
Write-Host ""
Write-Host "For GitHub Actions, base64-encode the keystore file into secret SARATHI_RELEASE_KEYSTORE_B64 and set the password secrets."
Write-Host "Never commit release-secrets/ or signing.properties."
