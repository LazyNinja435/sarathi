#Requires -Version 5.1

<#

.SYNOPSIS

  Publishes packaged dist assets to a GitHub Release using the gh CLI.

.PARAMETER VersionName

  Default 0.1.0 (APP_ONLY / FULL_MODEL; ignored when -Tag is set explicitly).

.PARAMETER VersionCode

  Optional; ignored by this script (use when copy-pasting alongside package-github-release.ps1).

.PARAMETER Tag

  Default v0.1.0 (or v + VersionName if omitted). For MODEL_ONLY use e.g. model-gemma-4-e2b.

.PARAMETER Repo

  Default LazyNinja435/sarathi

.PARAMETER ReleaseType

  APP_ONLY: uploads APK, sarathi-latest.json, checksums.sha256, RELEASE_NOTES.md.

  FULL_MODEL: also uploads model .partNNN chunk files from dist/github-release.

  MODEL_ONLY: uploads model-latest.json, checksums.sha256, and chunk files from dist/github-model-release.

.PARAMETER SkipModel

  Deprecated alias for -ReleaseType APP_ONLY (if both set, ReleaseType wins).

.PARAMETER Draft

  Passes --draft to gh release create.

.PARAMETER Prerelease

  Passes --prerelease to gh release create.

.PARAMETER ClobberUpload

  When set, passes --clobber to gh release upload for replacing assets.

.EXAMPLE

  .\scripts\publish-github-release.ps1 -ReleaseType MODEL_ONLY -Tag model-gemma-4-e2b

.EXAMPLE

  .\scripts\publish-github-release.ps1 -VersionName 0.1.0 -ReleaseType APP_ONLY

#>

param(

    [string] $VersionName = "0.1.0",

    [int] $VersionCode = -1,

    [string] $Tag = "",

    [string] $Repo = "LazyNinja435/sarathi",

    [string] $TargetBranch = "master",

    [ValidateSet("APP_ONLY", "FULL_MODEL", "MODEL_ONLY")]

    [string] $ReleaseType = "APP_ONLY",

    [switch] $SkipModel,

    [switch] $Draft,

    [switch] $Prerelease,

    [switch] $ClobberUpload

)



$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")



$c = Get-Command gh -ErrorAction SilentlyContinue

if ($null -ne $c -and (Test-Path $c.Path)) {

    $ghExe = $c.Path

} else {

    $ghExe = Join-Path ${env:ProgramFiles} "GitHub CLI\gh.exe"

    if (-not (Test-Path $ghExe)) {

        throw "GitHub CLI (gh) not found. Install from https://cli.github.com/ or add gh to PATH."

    }

    $env:PATH = "$(Split-Path $ghExe);$env:PATH"

}



function Invoke-Gh {

    param([string[]] $Arguments)

    & $ghExe @Arguments

    if ($LASTEXITCODE -ne 0) {

        throw ("gh failed (exit {0}): gh {1}" -f $LASTEXITCODE, ($Arguments -join " "))

    }

}



function Test-GhReleaseExists([string] $ReleaseTag) {

    & $ghExe release view $ReleaseTag --repo $Repo 2>$null

    return ($LASTEXITCODE -eq 0)

}



if ($SkipModel -and -not $PSBoundParameters.ContainsKey("ReleaseType")) {

    $ReleaseType = "APP_ONLY"

}



$Dist = if ($ReleaseType -eq "MODEL_ONLY") {

    Join-Path $RepoRoot "dist\github-model-release"

} else {

    Join-Path $RepoRoot "dist\github-release"

}



if ($ReleaseType -eq "MODEL_ONLY") {

    if ([string]::IsNullOrWhiteSpace($Tag)) { $Tag = "model-gemma-4-e2b" }

} else {

    if ([string]::IsNullOrWhiteSpace($Tag)) { $Tag = "v$VersionName" }

}



& $ghExe auth status

if ($LASTEXITCODE -ne 0) {

    throw "GitHub CLI is not authenticated. Run: gh auth login"

}



if (-not (Test-Path $Dist)) { throw "Missing dist folder: $Dist. Run scripts/package-github-release.ps1 first." }



$checksums = Join-Path $Dist "checksums.sha256"

if (-not (Test-Path $checksums)) { throw "Missing required file: $checksums" }



$existing = Test-GhReleaseExists $Tag



if ($ReleaseType -eq "MODEL_ONLY") {

    $manifest = Join-Path $Dist "model-latest.json"

    if (-not (Test-Path $manifest)) { throw "Missing required file: $manifest" }



    $parts = Get-ChildItem -Path $Dist -File | Where-Object { $_.Name -like "*.part*" }

    if (-not $parts -or $parts.Count -eq 0) { throw "MODEL_ONLY publish: no model chunk files in $Dist." }



    if (-not $existing) {

        $notesPath = Join-Path $Dist "MODEL_RELEASE_NOTES.md"

        @(

            "# Sarathi offline model (chunked)",

            "",

            "Install or update the app separately. In Sarathi Settings, use Download offline model; the app fetches this manifest when the APK release is app-only.",

            ""

        ) | Set-Content -Path $notesPath -Encoding utf8

        $createArgs = @("release", "create", $Tag, "--repo", $Repo, "--target", $TargetBranch, "--title", ("Sarathi model " + $Tag), "--notes-file", $notesPath, "--latest=false")

        if ($Draft) { $createArgs += "--draft" }

        if ($Prerelease) { $createArgs += "--prerelease" }

        Write-Host ("Creating release " + $Tag + " ...")

        Invoke-Gh $createArgs

    }



    $uploadArgs = @("release", "upload", $Tag, "--repo", $Repo)

    if ($ClobberUpload) { $uploadArgs += "--clobber" }

    elseif ($existing) {

        throw "Release $Tag already exists. Re-run with -ClobberUpload to replace assets, or use a new tag."

    }



    $assets = @($manifest, $checksums) + ($parts | ForEach-Object { $_.FullName })

    Write-Host "Uploading MODEL_ONLY assets..."

    Invoke-Gh ($uploadArgs + $assets)



    Write-Host "Done. Model manifest URL:"

    Write-Host ("https://github.com/{0}/releases/download/{1}/model-latest.json" -f $Repo, $Tag)

    exit 0

}



$manifest = Join-Path $Dist "sarathi-latest.json"

$apk = Join-Path $Dist ("sarathi-v{0}.apk" -f $VersionName)

$notes = Join-Path $Dist "RELEASE_NOTES.md"



foreach ($p in @($manifest, $apk, $checksums, $notes)) {

    if (-not (Test-Path $p)) { throw "Missing required file: $p" }

}



if (-not $existing) {

    $createArgs = @("release", "create", $Tag, "--repo", $Repo, "--target", $TargetBranch, "--title", ("Sarathi " + $Tag), "--notes-file", $notes, "--latest")

    if ($Draft) { $createArgs += "--draft" }

    if ($Prerelease) { $createArgs += "--prerelease" }

    Write-Host ("Creating release " + $Tag + " ...")

    Invoke-Gh $createArgs

}



$uploadArgs = @("release", "upload", $Tag, "--repo", $Repo)

if ($ClobberUpload) { $uploadArgs += "--clobber" }

elseif ($existing) {

    throw "Release $Tag already exists. Re-run with -ClobberUpload to replace assets, or use a new tag."

}



$assets = @($apk, $manifest, $checksums, $notes)

if ($ReleaseType -eq "FULL_MODEL") {

    $parts = Get-ChildItem -Path $Dist -File | Where-Object { $_.Name -like "*.part*" }

    if (-not $parts -or $parts.Count -eq 0) { throw "FULL_MODEL publish: no model chunk files in $Dist." }

    $assets += $parts | ForEach-Object { $_.FullName }

}



Write-Host ("Uploading assets (" + $ReleaseType + ")...")

Invoke-Gh ($uploadArgs + $assets)



Write-Host "Done. Manifest URL:"

Write-Host ("https://github.com/{0}/releases/latest/download/sarathi-latest.json" -f $Repo)

