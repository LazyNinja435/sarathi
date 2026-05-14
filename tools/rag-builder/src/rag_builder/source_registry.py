from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from rag_builder.paths import knowledge_root


def load_global_source_manifest() -> dict[str, Any]:
    p = knowledge_root() / "sources" / "source_manifest.json"
    with p.open(encoding="utf-8") as f:
        return json.load(f)


def source_ids() -> set[str]:
    data = load_global_source_manifest()
    return {s["id"] for s in data.get("sources", [])}


def get_source(source_id: str) -> dict[str, Any] | None:
    data = load_global_source_manifest()
    for s in data.get("sources", []):
        if s.get("id") == source_id:
            return s
    return None
