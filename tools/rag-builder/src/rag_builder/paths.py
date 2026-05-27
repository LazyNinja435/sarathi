from __future__ import annotations

import os
from pathlib import Path


def repo_root() -> Path:
    """Repository root (parent of `tools/`)."""
    # tools/rag-builder/src/rag_builder/paths.py → parents[4] == repo root
    return Path(__file__).resolve().parents[4]


def knowledge_root() -> Path:
    return repo_root() / "shared" / "knowledge"


def rag_builder_root() -> Path:
    return repo_root() / "tools" / "rag-builder"


def config_dir() -> Path:
    return rag_builder_root() / "config"


def indexes_dir() -> Path:
    return knowledge_root() / "indexes"


def gita_raw_dir() -> Path:
    return knowledge_root() / "sources" / "gita" / "raw"


def gita_processed_dir() -> Path:
    return knowledge_root() / "sources" / "gita" / "processed"


def mahabharata_raw_dir() -> Path:
    return knowledge_root() / "sources" / "mahabharata" / "raw"


def mahabharata_processed_dir() -> Path:
    return knowledge_root() / "sources" / "mahabharata" / "processed"


def android_rag_assets_dir() -> Path:
    return repo_root() / "android" / "app" / "src" / "main" / "assets" / "rag"


def load_yaml(name: str) -> dict:
    import yaml

    p = config_dir() / name
    if not p.exists():
        return {}
    with p.open(encoding="utf-8") as f:
        return yaml.safe_load(f) or {}


def env_int(name: str, default: int | None = None) -> int | None:
    v = os.environ.get(name)
    if v is None or v.strip() == "":
        return default
    try:
        return int(v)
    except ValueError:
        return default
