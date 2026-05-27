"""One-off RAG SQLite validation; run: python android/verification/sqlite_rag_check.py"""
import os
import sqlite3

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
PATHS = [
    os.path.join(ROOT, "shared", "knowledge", "indexes", "sarathi_rag.sqlite"),
    os.path.join(ROOT, "android", "app", "src", "main", "assets", "rag", "sarathi_rag.sqlite"),
]

QUERIES = [
    ("documents", "SELECT COUNT(*) FROM documents"),
    ("gita", "SELECT COUNT(*) FROM documents WHERE collection='gita'"),
    ("mahabharata", "SELECT COUNT(*) FROM documents WHERE collection='mahabharata'"),
    ("sources", "SELECT COUNT(*) FROM sources"),
    ("concepts", "SELECT COUNT(*) FROM concepts"),
    ("documents_fts", "SELECT COUNT(*) FROM documents_fts"),
]


def main() -> None:
    for p in PATHS:
        print("===", p)
        print("exists", os.path.isfile(p))
        if not os.path.isfile(p):
            continue
        c = sqlite3.connect(f"file:{p}?mode=ro", uri=True)
        cur = c.cursor()
        for name, q in QUERIES:
            try:
                cur.execute(q)
                print(f"  {name}: {cur.fetchone()[0]}")
            except Exception as e:
                print(f"  {name}: ERR {e}")
        for label, q in [
            ("sample gita", "SELECT id, citation FROM documents WHERE collection='gita' LIMIT 5"),
            ("sample maha", "SELECT id, citation FROM documents WHERE collection='mahabharata' LIMIT 5"),
            ("fts dharma", "SELECT doc_id, citation FROM documents_fts WHERE documents_fts MATCH 'dharma' LIMIT 5"),
            ("fts karma", "SELECT doc_id, citation FROM documents_fts WHERE documents_fts MATCH 'karma' LIMIT 5"),
            ("fts fear", "SELECT doc_id, citation FROM documents_fts WHERE documents_fts MATCH 'fear' LIMIT 5"),
            (
                "fts phrase",
                'SELECT doc_id, citation FROM documents_fts WHERE documents_fts MATCH \'"Arjuna Krishna"\' LIMIT 5',
            ),
            (
                "fts AND",
                "SELECT doc_id, citation FROM documents_fts WHERE documents_fts MATCH 'Arjuna AND Krishna' LIMIT 5",
            ),
        ]:
            try:
                cur.execute(q)
                rows = cur.fetchall()
                print(f"  {label}: {len(rows)} rows")
                for r in rows[:3]:
                    print(f"    {r}")
            except Exception as e:
                print(f"  {label}: ERR {e}")
        c.close()


if __name__ == "__main__":
    main()
