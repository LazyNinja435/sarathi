import json
import sqlite3
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
DB_PATH = REPO_ROOT / "app" / "src" / "main" / "assets" / "rag" / "sarathi_rag.sqlite"
OUT_PATH = REPO_ROOT / "web" / "apps" / "frontend" / "public" / "rag" / "sarathi_rag.json"


def parse_json(value, fallback):
    if not value:
        return fallback
    try:
        return json.loads(value)
    except json.JSONDecodeError:
        return fallback


def main():
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row

    sources = {
        row["id"]: {
            "title": row["title"],
            "sourceUrl": row["source_url"],
        }
        for row in conn.execute("select id, title, source_url from sources")
    }

    documents = []
    for row in conn.execute(
        """
        select id, collection, work, title, citation, source_id, text, translation, themes_json
        from documents
        where collection in ('gita', 'mahabharata')
        order by collection, id
        """
    ):
        source = sources.get(row["source_id"], {})
        body = row["translation"] or row["text"] or ""
        if not body.strip():
            continue
        documents.append({
            "id": row["id"],
            "collection": row["collection"],
            "work": row["work"],
            "title": row["title"],
            "citation": row["citation"],
            "text": row["text"],
            "translation": row["translation"],
            "themes": parse_json(row["themes_json"], []),
            "sourceTitle": source.get("title", ""),
            "sourceUrl": source.get("sourceUrl", ""),
        })

    concepts = []
    for row in conn.execute(
        """
        select id, name, aliases_json, description, related_ids_json, search_terms_json
        from concepts
        order by id
        """
    ):
        concepts.append({
            "id": row["id"],
            "name": row["name"],
            "aliases": parse_json(row["aliases_json"], []),
            "description": row["description"],
            "relatedIds": parse_json(row["related_ids_json"], []),
            "searchTerms": parse_json(row["search_terms_json"], []),
        })

    OUT_PATH.write_text(json.dumps({
        "version": 1,
        "generatedFrom": "app/src/main/assets/rag/sarathi_rag.sqlite",
        "documents": documents,
        "concepts": concepts,
    }, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"Wrote {len(documents)} documents and {len(concepts)} concepts to {OUT_PATH}")


if __name__ == "__main__":
    main()
