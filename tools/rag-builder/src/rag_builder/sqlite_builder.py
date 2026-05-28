from __future__ import annotations

import json
import shutil
import sqlite3
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from rag_builder.paths import (
    android_rag_assets_dir,
    gita_processed_dir,
    indexes_dir,
    mahabharata_processed_dir,
)
from rag_builder.source_registry import load_global_source_manifest


def _connect(db_path: Path) -> sqlite3.Connection:
    db_path.parent.mkdir(parents=True, exist_ok=True)
    if db_path.exists():
        db_path.unlink()
    con = sqlite3.connect(str(db_path))
    con.execute("PRAGMA journal_mode=DELETE;")
    return con


def _init_schema(con: sqlite3.Connection) -> None:
    con.executescript(
        """
        CREATE TABLE sources (
          id TEXT PRIMARY KEY,
          collection TEXT,
          title TEXT,
          author_or_translator TEXT,
          year TEXT,
          source_url TEXT,
          license TEXT,
          notes TEXT,
          verified INTEGER
        );

        CREATE TABLE documents (
          id TEXT PRIMARY KEY,
          collection TEXT,
          work TEXT,
          title TEXT,
          section_path TEXT,
          citation TEXT,
          source_id TEXT,
          language TEXT,
          text TEXT,
          sanskrit TEXT,
          transliteration TEXT,
          translation TEXT,
          commentary TEXT,
          themes_json TEXT,
          entities_json TEXT,
          metadata_json TEXT
        );

        CREATE VIRTUAL TABLE documents_fts USING fts5(
          doc_id UNINDEXED,
          title,
          citation,
          text,
          sanskrit,
          transliteration,
          translation,
          commentary,
          themes_text,
          tokenize = 'porter'
        );

        CREATE TABLE concepts (
          id TEXT PRIMARY KEY,
          name TEXT,
          aliases_json TEXT,
          description TEXT,
          related_ids_json TEXT,
          search_terms_json TEXT
        );

        CREATE TABLE app_metadata (
          key TEXT PRIMARY KEY,
          value TEXT
        );

        CREATE TABLE document_embeddings (
          document_id TEXT PRIMARY KEY,
          model TEXT,
          dim INTEGER,
          vector BLOB
        );
        """
    )


def _insert_sources(con: sqlite3.Connection) -> None:
    data = load_global_source_manifest()
    for s in data.get("sources", []):
        con.execute(
            """INSERT INTO sources (id, collection, title, author_or_translator, year, source_url, license, notes, verified)
               VALUES (?,?,?,?,?,?,?,?,?)""",
            (
                s["id"],
                s.get("collection", ""),
                s.get("title", ""),
                s.get("author_or_translator", ""),
                str(s.get("year", "")),
                s.get("source_url", ""),
                s.get("license", ""),
                s.get("notes", ""),
                int(s.get("verified", 0)),
            ),
        )


def _insert_document(con: sqlite3.Connection, row: dict[str, Any]) -> None:
    themes_json = row["themes_json"]
    entities_json = row["entities_json"]
    themes = json.loads(themes_json) if themes_json else []
    themes_text = " ".join(themes) if isinstance(themes, list) else ""
    con.execute(
        """INSERT INTO documents (
            id, collection, work, title, section_path, citation, source_id, language,
            text, sanskrit, transliteration, translation, commentary,
            themes_json, entities_json, metadata_json
        ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
        (
            row["id"],
            row["collection"],
            row["work"],
            row["title"],
            row["section_path"],
            row["citation"],
            row["source_id"],
            row["language"],
            row["text"],
            row["sanskrit"],
            row["transliteration"],
            row["translation"],
            row["commentary"],
            themes_json,
            entities_json,
            row["metadata_json"],
        ),
    )
    con.execute(
        """INSERT INTO documents_fts (doc_id, title, citation, text, sanskrit, transliteration, translation, commentary, themes_text)
           VALUES (?,?,?,?,?,?,?,?,?)""",
        (
            row["id"],
            row["title"],
            row["citation"],
            row["text"],
            row["sanskrit"],
            row["transliteration"],
            row["translation"],
            row["commentary"],
            themes_text,
        ),
    )


def _load_gita_documents() -> list[dict[str, Any]]:
    p = gita_processed_dir() / "gita_verses.jsonl"
    if not p.exists():
        return []
    rows: list[dict[str, Any]] = []
    for line in p.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        o = json.loads(line)
        doc = _normalize_gita_row(o)
        ch = doc["chapter"]
        vs = doc["verse"]
        meta = {
            "chapter": ch,
            "verse": vs,
            "chapter_title": doc["chapter_title"],
            "teaching": doc.get("teaching", {}),
            "safety": doc.get("safety", {}),
        }
        full_text_parts = [doc["translation"]]
        if doc["sanskrit"]:
            full_text_parts.append(doc["sanskrit"])
        text_joined = "\n\n".join([x for x in full_text_parts if x])
        rows.append(
            {
                "id": doc["id"],
                "collection": doc["collection"],
                "work": doc["work"],
                "title": doc["citation"],
                "section_path": f"gita/{ch:02d}/{vs:03d}",
                "citation": doc["citation"],
                "source_id": doc["source_id"],
                "language": doc["language"],
                "text": text_joined,
                "sanskrit": doc["sanskrit"],
                "transliteration": doc["transliteration"],
                "translation": doc["translation"],
                "commentary": doc["search_text"],
                "themes_json": json.dumps(doc["themes"], ensure_ascii=False),
                "entities_json": json.dumps(doc["entities"], ensure_ascii=False),
                "metadata_json": json.dumps(meta, ensure_ascii=False),
            }
        )
    return rows


def _normalize_gita_row(o: dict[str, Any]) -> dict[str, Any]:
    if "source" not in o:
        return {
            "id": o["id"],
            "collection": o["collection"],
            "work": o["work"],
            "chapter": int(o["chapter"]),
            "verse": int(o["verse"]),
            "citation": o["citation"],
            "chapter_title": o.get("chapter_title", ""),
            "source_id": o["source_id"],
            "language": o.get("language", "en"),
            "sanskrit": o.get("sanskrit", ""),
            "transliteration": o.get("transliteration", ""),
            "translation": o.get("translation", ""),
            "search_text": o.get("search_text", ""),
            "themes": o.get("themes", []),
            "entities": o.get("entities", []),
            "teaching": {},
            "safety": {},
        }

    source = o.get("source", {})
    content = o.get("content", {})
    retrieval = o.get("retrieval", {})
    rag = o.get("rag", {})
    themes = _unique_strings(
        retrieval.get("primary_topics", [])
        + retrieval.get("secondary_topics", [])
        + retrieval.get("intent_matches", [])
        + retrieval.get("spiritual_concepts", [])
    )

    # Infer chapter, verse, and citation from record id (e.g. BG-1-1 -> chapter=1, verse=1, citation='Bhagavad Gita 1.1')
    chapter = source.get("chapter")
    verse = source.get("verse")
    citation = source.get("citation")

    if (chapter is None or verse is None or citation is None) and o["id"].startswith("BG-"):
        parts = o["id"].split("-")
        if len(parts) == 3:
            if chapter is None:
                chapter = int(parts[1])
            if verse is None:
                verse = int(parts[2])
            if citation is None:
                citation = f"Bhagavad Gita {parts[1]}.{parts[2]}"

    chapter = chapter or 1
    verse = verse or 1
    citation = citation or o["id"]

    return {
        "id": o["id"],
        "collection": "gita",
        "work": source.get("text") or "Bhagavad Gita",
        "chapter": chapter,
        "verse": verse,
        "citation": citation,
        "chapter_title": source.get("chapter_title", ""),
        "source_id": source.get("source_id") or "besant_wikisource_1922",
        "language": o.get("metadata", {}).get("language", "en"),
        "sanskrit": content.get("sanskrit", ""),
        "transliteration": content.get("transliteration", ""),
        "translation": content.get("translation", ""),
        "search_text": rag.get("search_text", ""),
        "themes": themes,
        "entities": _unique_strings(["Krishna", "Arjuna"]),
        "teaching": o.get("teaching", {}),
        "safety": o.get("safety", {}),
    }


def _unique_strings(values: list[Any]) -> list[str]:
    out: list[str] = []
    seen: set[str] = set()
    for value in values:
        text = str(value).strip()
        if text and text not in seen:
            seen.add(text)
            out.append(text)
    return out


def _load_mahabharata_documents() -> list[dict[str, Any]]:
    p = mahabharata_processed_dir() / "mahabharata_chunks.jsonl"
    if not p.exists():
        return []
    rows: list[dict[str, Any]] = []
    for line in p.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        o = json.loads(line)
        meta = {
            "parva_number": o.get("parva_number"),
            "parva_name": o.get("parva_name"),
            "section": o.get("section"),
            "subsection": o.get("subsection"),
            "paragraph_index": o.get("paragraph_index"),
            "contains_gita": o.get("contains_gita", False),
        }
        rows.append(
            {
                "id": o["id"],
                "collection": o["collection"],
                "work": o["work"],
                "title": o["citation"],
                "section_path": f"mahabharata/{o['parva_number']:02d}/{o.get('section','')}/p{o.get('paragraph_index', 0)}",
                "citation": o["citation"],
                "source_id": o["source_id"],
                "language": o.get("language", "en"),
                "text": o.get("text", ""),
                "sanskrit": "",
                "transliteration": "",
                "translation": "",
                "commentary": "",
                "themes_json": json.dumps(o.get("themes", []), ensure_ascii=False),
                "entities_json": json.dumps(o.get("entities", []), ensure_ascii=False),
                "metadata_json": json.dumps(meta, ensure_ascii=False),
            }
        )
    return rows


def _insert_concepts(con: sqlite3.Connection) -> int:
    p = gita_processed_dir() / "gita_concepts.jsonl"
    if not p.exists():
        return 0
    n = 0
    for line in p.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        c = json.loads(line)
        con.execute(
            """INSERT INTO concepts (id, name, aliases_json, description, related_ids_json, search_terms_json)
               VALUES (?,?,?,?,?,?)""",
            (
                c["id"],
                c["name"],
                json.dumps(c.get("aliases", []), ensure_ascii=False),
                c.get("description", ""),
                json.dumps(c.get("related_verses", []), ensure_ascii=False),
                json.dumps(c.get("search_terms", []), ensure_ascii=False),
            ),
        )
        n += 1
    return n


def build_sqlite() -> dict[str, Any]:
    idx = indexes_dir()
    idx.mkdir(parents=True, exist_ok=True)
    db_path = idx / "sarathi_rag.sqlite"
    g_rows = _load_gita_documents()
    m_rows = _load_mahabharata_documents()
    concept_count = 0
    con = _connect(db_path)
    try:
        _init_schema(con)
        _insert_sources(con)
        for r in g_rows + m_rows:
            _insert_document(con, r)
        concept_count = _insert_concepts(con)
        now = datetime.now(timezone.utc).isoformat()
        con.execute("INSERT INTO app_metadata (key, value) VALUES (?,?)", ("generated_at_utc", now))
        con.execute("INSERT INTO app_metadata (key, value) VALUES (?,?)", ("schema_version", "1"))
        con.commit()
    finally:
        con.close()

    manifest = {
        "database": "sarathi_rag.sqlite",
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "gita_documents": len(g_rows),
        "mahabharata_documents": len(m_rows),
        "concepts": concept_count,
        "notes": "FTS5 keyword index; embeddings table reserved for future use.",
    }
    (idx / "sarathi_rag_manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")

    assets = android_rag_assets_dir()
    assets.mkdir(parents=True, exist_ok=True)
    shutil.copy2(db_path, assets / "sarathi_rag.sqlite")
    shutil.copy2(idx / "sarathi_rag_manifest.json", assets / "sarathi_rag_manifest.json")

    return {
        "sqlite_path": str(db_path),
        "gita_documents": len(g_rows),
        "mahabharata_documents": len(m_rows),
        "concepts": concept_count,
    }
