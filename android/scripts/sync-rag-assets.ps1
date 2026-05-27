$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$knowledgeIndex = Join-Path $repoRoot "shared\knowledge\indexes"
$androidAssets = Join-Path $repoRoot "android\app\src\main\assets\rag"
$verifier = Join-Path $repoRoot "tools\verify_knowledge_artifacts.py"

$db = Join-Path $knowledgeIndex "sarathi_rag.sqlite"
$manifest = Join-Path $knowledgeIndex "sarathi_rag_manifest.json"

if (!(Test-Path $db)) {
    throw "Missing canonical RAG DB: $db"
}
if (!(Test-Path $manifest)) {
    throw "Missing canonical RAG manifest: $manifest"
}

New-Item -ItemType Directory -Force -Path $androidAssets | Out-Null
Copy-Item -LiteralPath $db -Destination (Join-Path $androidAssets "sarathi_rag.sqlite") -Force
Copy-Item -LiteralPath $manifest -Destination (Join-Path $androidAssets "sarathi_rag_manifest.json") -Force

python $verifier
