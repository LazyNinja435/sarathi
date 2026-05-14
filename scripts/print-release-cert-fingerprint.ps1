#Requires -Version 5.1
<#
.SYNOPSIS
  Prints SHA-256 certificate fingerprint for the configured release keystore (env vars).
#>
$ErrorActionPreference = "Stop"

$ks = $env:SARATHI_KEYSTORE_PATH
$pass = $env:SARATHI_KEYSTORE_PASSWORD
$alias = $env:SARATHI_KEY_ALIAS

if ([string]::IsNullOrWhiteSpace($ks) -or [string]::IsNullOrWhiteSpace($pass) -or [string]::IsNullOrWhiteSpace($alias)) {
    throw "Set SARATHI_KEYSTORE_PATH, SARATHI_KEYSTORE_PASSWORD, and SARATHI_KEY_ALIAS first."
}
if (-not (Test-Path $ks)) { throw "Keystore not found: $ks" }

$keytool = "keytool"
& $keytool -list -v -keystore $ks -storepass $pass -alias $alias
