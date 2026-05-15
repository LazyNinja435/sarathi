#Requires -Version 5.1

<#

.SYNOPSIS

  Builds release assets for GitHub: APP_ONLY, FULL_MODEL, or MODEL_ONLY.

.PARAMETER VersionName

  When set with VersionCode, updates sarathi-version.properties before building (APP_ONLY / FULL_MODEL only).

.PARAMETER VersionCode

  When set with VersionName, updates sarathi-version.properties before building.

.PARAMETER ReleaseType

  APP_ONLY (default): APK + sarathi-latest.json + checksums; no chunks; manifest includes modelSource.externalManifestUrl.

  FULL_MODEL: APK + sarathi-latest.json with inline chunks + checksums.

  MODEL_ONLY: model-latest.json + chunks + checksums under dist/github-model-release (no APK).

.PARAMETER Repo

  GitHub repo slug for manifest URLs (default LazyNinja435/sarathi).

.PARAMETER ModelReleaseTag

  Tag for MODEL_ONLY manifest URLs (default model-gemma-4-e2b).

.EXAMPLE

  .\scripts\package-github-release.ps1 -ReleaseType MODEL_ONLY

.EXAMPLE

  .\scripts\publish-github-release.ps1 -ReleaseType MODEL_ONLY -Tag model-gemma-4-e2b

.EXAMPLE

  .\scripts\package-github-release.ps1 -VersionName 0.1.0 -VersionCode 1 -ReleaseType APP_ONLY

.EXAMPLE

  .\scripts\publish-github-release.ps1 -VersionName 0.1.0 -ReleaseType APP_ONLY

#>

param(

    [string] $Repo = "LazyNinja435/sarathi",

    [string] $VersionName = "",

    [int] $VersionCode = -1,

    [ValidateSet("APP_ONLY", "FULL_MODEL", "MODEL_ONLY")]

    [string] $ReleaseType = "APP_ONLY",

    [string] $ModelReleaseTag = "model-gemma-4-e2b"

)



$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")

$DistApp = Join-Path $RepoRoot "dist\github-release"

$DistModel = Join-Path $RepoRoot "dist\github-model-release"

$ModelSrc = Join-Path $RepoRoot "local-models\gemma4-e2b\gemma-4-E2B-it.litertlm"

$PropsPath = Join-Path $RepoRoot "sarathi-version.properties"

$javaHome = "C:\Program Files\Android\Android Studio\jbr"

if (Test-Path $javaHome) { $env:JAVA_HOME = $javaHome }



if (-not (Test-Path $ModelSrc)) {

    throw "Model not found at $ModelSrc - place gemma-4-E2B-it.litertlm there before packaging (metadata only for APP_ONLY)."

}



if ($ReleaseType -ne "MODEL_ONLY") {

    if (-not [string]::IsNullOrWhiteSpace($VersionName) -and $VersionCode -ge 0) {

        $propsLines = @(

            "# Bump for each store / GitHub release.",

            "SARATHI_VERSION_CODE=$VersionCode",

            "SARATHI_VERSION_NAME=$VersionName"

        )

        Set-Content -Path $PropsPath -Value ($propsLines -join "`n") -Encoding utf8

        Write-Host "Updated $PropsPath -> $VersionName ($VersionCode)"

    }



    & (Join-Path $PSScriptRoot "build-release-apk.ps1")



    $Dist = $DistApp

    New-Item -ItemType Directory -Force -Path $Dist | Out-Null



    if ($ReleaseType -eq "FULL_MODEL") {

        Get-ChildItem -Path $Dist -File -ErrorAction SilentlyContinue |

            Where-Object { $_.Name -like "*.part*" } |

            ForEach-Object { Remove-Item $_.FullName -Force }

    }



    $py = "py"

    try { & $py -V | Out-Null } catch { $py = "python"; & $py -V | Out-Null }



    if ($ReleaseType -eq "FULL_MODEL") {

        & $py (Join-Path $RepoRoot "tools\release\split_model.py") --input $ModelSrc --output-dir $Dist --chunk-bytes 943718400

        if ($LASTEXITCODE -ne 0) { throw "split_model.py failed." }

    }



    $releaseArg = if ($ReleaseType -eq "APP_ONLY") { "app_only" } else { "full_model" }
    & $py (Join-Path $RepoRoot "tools\release\create_release_manifest.py") --repo-root $RepoRoot --dist-dir $Dist --model-source $ModelSrc --repo $Repo --release-type $releaseArg
    if ($LASTEXITCODE -ne 0) { throw "create_release_manifest.py failed." }



    & $py (Join-Path $RepoRoot "tools\release\write_checksums.py") --dist-dir $Dist

    if ($LASTEXITCODE -ne 0) { throw "write_checksums.py failed." }



    & $py (Join-Path $RepoRoot "tools\release\verify_release_manifest.py") --dist-dir $Dist

    if ($LASTEXITCODE -ne 0) { throw "verify_release_manifest.py failed." }



    $notesPath = Join-Path $Dist "RELEASE_NOTES.md"

    if ($ReleaseType -eq "APP_ONLY") {

        @(

            "# Sarathi release ($ReleaseType)",

            "",

            "## Offline model",

            "",

            "This APK release does not bundle model chunks. Download the offline Gemma model from the stable model release (see modelSource.externalManifestUrl in sarathi-latest.json) via Settings - Download offline model.",

            "",

            "## Updates",

            "",

            "Use Settings - Update Sarathi to download a signed APK only. Model files in app-private storage are not removed by the in-app updater.",

            ""

        ) | Set-Content -Path $notesPath -Encoding utf8

    } else {

        @(

            "# Sarathi release ($ReleaseType)",

            "",

            "## Offline model",

            "",

            "This release includes a new offline model download. The app will ask before downloading it.",

            "",

            "## Updates",

            "",

            "The APK update and the large model download are separate steps; neither runs silently.",

            ""

        ) | Set-Content -Path $notesPath -Encoding utf8

    }



    Write-Host ""

    Write-Host "dist/github-release ready ($ReleaseType). Example publish:"

    Write-Host ('  .\scripts\publish-github-release.ps1 -Repo "' + $Repo + '" -ReleaseType ' + $ReleaseType)

    exit 0

}



# --- MODEL_ONLY ---

$Dist = $DistModel

New-Item -ItemType Directory -Force -Path $Dist | Out-Null

Get-ChildItem -Path $Dist -File -ErrorAction SilentlyContinue |

    Where-Object { $_.Name -like "*.part*" -or $_.Name -eq "model-latest.json" -or $_.Name -eq "checksums.sha256" } |

    ForEach-Object { Remove-Item $_.FullName -Force }



$py = "py"

try { & $py -V | Out-Null } catch { $py = "python"; & $py -V | Out-Null }



& $py (Join-Path $RepoRoot "tools\release\split_model.py") --input $ModelSrc --output-dir $Dist --chunk-bytes 943718400

if ($LASTEXITCODE -ne 0) { throw "split_model.py failed." }



& $py (Join-Path $RepoRoot "tools\release\create_release_manifest.py") --repo-root $RepoRoot --dist-dir $Dist --model-source $ModelSrc --repo $Repo --release-type model_only --model-release-tag $ModelReleaseTag
if ($LASTEXITCODE -ne 0) { throw "create_release_manifest.py failed." }



& $py (Join-Path $RepoRoot "tools\release\write_checksums.py") --dist-dir $Dist

if ($LASTEXITCODE -ne 0) { throw "write_checksums.py failed." }



& $py (Join-Path $RepoRoot "tools\release\verify_release_manifest.py") --dist-dir $Dist

if ($LASTEXITCODE -ne 0) { throw "verify_release_manifest.py failed." }



Write-Host ""

Write-Host "dist/github-model-release ready (MODEL_ONLY). Example publish:"

Write-Host ('  .\scripts\publish-github-release.ps1 -Repo "' + $Repo + '" -ReleaseType MODEL_ONLY -Tag ' + $ModelReleaseTag)

