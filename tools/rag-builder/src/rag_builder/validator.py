from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from rag_builder.paths import gita_processed_dir, indexes_dir, mahabharata_processed_dir
from rag_builder.source_registry import source_ids


def _read_jsonl(path: Path) -> list[dict[str, Any]]:
    if not path.exists():
        return []
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.strip():
            rows.append(json.loads(line))
    return rows


def validate_corpus() -> dict[str, Any]:
    warnings: list[str] = []
    errors: list[str] = []

    gita_path = gita_processed_dir() / "gita_verses.jsonl"
    mbh_path = mahabharata_processed_dir() / "mahabharata_chunks.jsonl"
    g_verses = _read_jsonl(gita_path)
    m_chunks = _read_jsonl(mbh_path)

    ids: set[str] = set()
    for row in g_verses + m_chunks:
        rid = row.get("id")
        if not rid:
            errors.append("Row missing id")
            continue
        if rid in ids:
            errors.append(f"Duplicate id: {rid}")
        ids.add(rid)

    known_sources = source_ids()

    gita_chapter_verse: set[tuple[int, int]] = set()
    for row in g_verses:
        normalized = _gita_fields(row)
        for field in ("translation", "citation", "source_id", "license", "search_text"):
            if not str(normalized.get(field, "")).strip():
                errors.append(f"gita row {row.get('id')}: empty {field}")
        if normalized.get("source_id") not in known_sources:
            errors.append(f"gita row {row.get('id')}: unknown source_id {normalized.get('source_id')}")
        try:
            ch = int(normalized["chapter"])
            vs = int(normalized["verse"])
        except (KeyError, TypeError, ValueError):
            errors.append(f"gita row {row.get('id')}: bad chapter/verse")
            continue
        if ch < 1 or ch > 18:
            warnings.append(f"gita row {row.get('id')}: chapter {ch} out of 1..18")
        if (ch, vs) in gita_chapter_verse:
            warnings.append(f"Duplicate chapter/verse: {ch}.{vs}")
        gita_chapter_verse.add((ch, vs))

    for row in m_chunks:
        for field in ("text", "citation", "source_id", "license", "search_text"):
            if not str(row.get(field, "")).strip():
                errors.append(f"mahabharata row {row.get('id')}: empty {field}")
        if row.get("source_id") not in known_sources:
            errors.append(f"mahabharata row {row.get('id')}: unknown source_id {row.get('source_id')}")
        pn = row.get("parva_number")
        if pn is not None and (int(pn) < 1 or int(pn) > 18):
            warnings.append(f"mahabharata row {row.get('id')}: parva_number {pn}")

    sqlite_path = indexes_dir() / "sarathi_rag.sqlite"
    if sqlite_path.exists():
        import sqlite3

        con = sqlite3.connect(f"file:{sqlite_path}?mode=ro", uri=True)
        try:
            cur = con.execute("SELECT COUNT(*) FROM documents")
            doc_count = cur.fetchone()[0]
            cur = con.execute("SELECT COUNT(*) FROM documents_fts")
            fts_count = cur.fetchone()[0]
            if doc_count != fts_count:
                errors.append(f"documents ({doc_count}) vs fts ({fts_count}) row mismatch")
        finally:
            con.close()
    else:
        warnings.append("sarathi_rag.sqlite not found under shared/knowledge/indexes/")

    return {
        "ok": len(errors) == 0,
        "errors": errors,
        "warnings": warnings,
        "gita_verses": len(g_verses),
        "mahabharata_chunks": len(m_chunks),
    }


def _gita_fields(row: dict[str, Any]) -> dict[str, Any]:
    if "source" not in row:
        return {
            "chapter": row.get("chapter"),
            "verse": row.get("verse"),
            "translation": row.get("translation"),
            "citation": row.get("citation"),
            "source_id": row.get("source_id"),
            "license": row.get("license"),
            "search_text": row.get("search_text"),
        }
    source = row.get("source", {})
    return {
        "chapter": source.get("chapter"),
        "verse": source.get("verse"),
        "translation": row.get("content", {}).get("translation"),
        "citation": source.get("citation"),
        "source_id": source.get("source_id"),
        "license": source.get("license"),
        "search_text": row.get("rag", {}).get("search_text"),
    }


def write_build_report(
    gita_val: dict[str, Any],
    mbh_val: dict[str, Any],
    sqlite_info: dict[str, Any],
    download_warnings: list[str],
) -> None:
    from datetime import datetime, timezone

    report = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "gita_verses_imported": gita_val.get("verse_count", sqlite_info.get("gita_documents", 0)),
        "expected_gita_verse_count": gita_val.get("expected_verse_count", 700),
        "gita_complete_flag": bool(gita_val.get("complete", sqlite_info.get("gita_documents") == 700)),
        "mahabharata_chunks_imported": mbh_val.get("chunk_count", sqlite_info.get("mahabharata_documents", 0)),
        "mahabharata_parvas_detected": mbh_val.get("parvas_detected", 0),
        "mahabharata_sections_detected": mbh_val.get("sections_detected", 0),
        "missing_mahabharata_parvas": mbh_val.get("missing_parva_numbers", []),
        "source_coverage": {
            "gita_primary": "besant_wikisource_1922",
            "mahabharata_primary": "ganguli_aasi_github",
        },
        "sqlite": sqlite_info,
        "warnings": (gita_val.get("warnings") or [])
        + (mbh_val.get("warnings") or [])
        + (download_warnings or []),
    }
    (indexes_dir() / "build_report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
