import hashlib
import json
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
KNOWLEDGE_INDEX_DIR = REPO_ROOT / "knowledge" / "indexes"
ANDROID_RAG_DIR = REPO_ROOT / "app" / "src" / "main" / "assets" / "rag"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def require_file(path: Path) -> None:
    if not path.is_file():
        raise SystemExit(f"Missing required artifact: {path.relative_to(REPO_ROOT)}")


def assert_same(left: Path, right: Path) -> None:
    require_file(left)
    require_file(right)
    left_hash = sha256(left)
    right_hash = sha256(right)
    if left_hash != right_hash:
        raise SystemExit(
            "Artifact drift detected:\n"
            f"  source: {left.relative_to(REPO_ROOT)} {left_hash}\n"
            f"  copy:   {right.relative_to(REPO_ROOT)} {right_hash}"
        )


def main() -> None:
    knowledge_db = KNOWLEDGE_INDEX_DIR / "sarathi_rag.sqlite"
    knowledge_manifest = KNOWLEDGE_INDEX_DIR / "sarathi_rag_manifest.json"
    android_db = ANDROID_RAG_DIR / "sarathi_rag.sqlite"
    android_manifest = ANDROID_RAG_DIR / "sarathi_rag_manifest.json"

    assert_same(knowledge_db, android_db)
    assert_same(knowledge_manifest, android_manifest)

    print("Knowledge artifacts verified:")
    print(f"  Android DB matches knowledge DB: {sha256(knowledge_db)}")
    print(f"  Android manifest matches knowledge manifest: {sha256(knowledge_manifest)}")


if __name__ == "__main__":
    main()
