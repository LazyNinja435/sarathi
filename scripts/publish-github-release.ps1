#Requires -Version 5.1
<#
.SYNOPSIS
  Publishes dist/github-release assets to a GitHub Release using the gh CLI.
.PARAMETER VersionName
  Default 0.1.0
.PARAMETER Tag
  Default v0.1.0 (or v + VersionName if omitted)
.PARAMETER Repo
  Default LazyNinja435/sarathi
.PARAMETER SkipModel
  When set, uploads APK + manifest + checksums only (no .partNNN chunks).
.PARAMETER Draft
  Passes --draft to gh release create.
.PARAMETER Prerelease
  Passes --prerelease to gh release create.
.PARAMETER ClobberUpload
  When set, passes --clobber to gh release upload for replacing assets.
#>
param(
    [string] $VersionName = "0.1.0",
    [string] $Tag = "",
    [string] $Repo = "LazyNinja435/sarathi",
    [switch] $SkipModel,
    [switch] $Draft,
    [switch] $Prerelease,
    [switch] $ClobberUpload
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$Dist = Join-Path $RepoRoot "dist\github-release"

if ([string]::IsNullOrWhiteSpace($Tag)) { $Tag = "v$VersionName" }

gh auth status 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "GitHub CLI (gh) is not installed or not authenticated. Install gh and run: gh auth login"
}

if (-not (Test-Path $Dist)) { throw "Missing dist folder: $Dist. Run scripts/package-github-release.ps1 first." }

$manifest = Join-Path $Dist "sarathi-latest.json"
$apk = Join-Path $Dist ("sarathi-v{0}.apk" -f $VersionName)
$checksums = Join-Path $Dist "checksums.sha256"
$notes = Join-Path $Dist "RELEASE_NOTES.md"

foreach ($p in @($manifest, $apk, $checksums, $notes)) {
    if (-not (Test-Path $p)) { throw "Missing required file: $p" }
}

$existing = $false
gh release view $Tag --repo $Repo 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) { $existing = $true }

if (-not $existing) {
    $args = @("release", "create", $Tag, "--repo", $Repo, "--title", ("Sarathi " + $Tag), "--notes-file", $notes)
    if ($Draft) { $args += "--draft" }
    if ($Prerelease) { $args += "--prerelease" }
    Write-Host ("Creating release " + $Tag + " ...")
    gh @args
}

$uploadArgs = @("release", "upload", $Tag, "--repo", $Repo)
if ($ClobberUpload) { $uploadArgs += "--clobber" }
elseif ($existing) {
    throw "Release $Tag already exists. Re-run with -ClobberUpload to replace assets, or use a new tag."
}

$assets = @($apk, $manifest, $checksums)
if (-not $SkipModel) {
    $parts = Get-ChildItem -Path $Dist -File | Where-Object { $_.Name -like "*.part*" }
    if (-not $parts -or $parts.Count -eq 0) { throw "No model chunk files found in $Dist (omit -SkipModel or run packaging)." }
    $assets += $parts | ForEach-Object { $_.FullName }
}

Write-Host "Uploading assets..."
$uploadCmd = $uploadArgs + $assets
gh @uploadCmd

Write-Host "Done. Manifest URL:"
Write-Host ("https://github.com/{0}/releases/latest/download/sarathi-latest.json" -f $Repo)
