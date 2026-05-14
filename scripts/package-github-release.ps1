#Requires -Version 5.1
<#
.SYNOPSIS
  Builds release APK, splits Gemma model, generates manifest + checksums, verifies locally.
#>
param(
    [string] $Repo = "LazyNinja435/sarathi"
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$Dist = Join-Path $RepoRoot "dist\github-release"
$ModelSrc = Join-Path $RepoRoot "local-models\gemma4-e2b\gemma-4-E2B-it.litertlm"
$NotesTemplate = Join-Path $RepoRoot "tools\release\RELEASE_NOTES.template.md"
$javaHome = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path $javaHome) { $env:JAVA_HOME = $javaHome }

if (-not (Test-Path $ModelSrc)) {
    throw "Model not found at $ModelSrc — place gemma-4-E2B-it.litertlm there before packaging."
}

& (Join-Path $PSScriptRoot "build-release-apk.ps1")

New-Item -ItemType Directory -Force -Path $Dist | Out-Null
Copy-Item $NotesTemplate (Join-Path $Dist "RELEASE_NOTES.md") -Force

$py = "py"
try { & $py -V | Out-Null } catch { $py = "python"; & $py -V | Out-Null }

& $py (Join-Path $RepoRoot "tools\release\split_model.py") --input $ModelSrc --output-dir $Dist --chunk-bytes 943718400
if ($LASTEXITCODE -ne 0) { throw "split_model.py failed." }

& $py (Join-Path $RepoRoot "tools\release\create_release_manifest.py") --repo-root $RepoRoot --dist-dir $Dist --model-source $ModelSrc --repo $Repo
if ($LASTEXITCODE -ne 0) { throw "create_release_manifest.py failed." }

& $py (Join-Path $RepoRoot "tools\release\write_checksums.py") --dist-dir $Dist
if ($LASTEXITCODE -ne 0) { throw "write_checksums.py failed." }

& $py (Join-Path $RepoRoot "tools\release\verify_release_manifest.py") --dist-dir $Dist
if ($LASTEXITCODE -ne 0) { throw "verify_release_manifest.py failed." }

Write-Host ""
Write-Host "dist/github-release is ready. Example publish (after gh auth login):"
Write-Host ('  .\scripts\publish-github-release.ps1 -Repo "' + $Repo + '"')
