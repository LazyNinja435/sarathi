import json
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

from rag_builder.sqlite_builder import _load_gita_documents
from rag_builder.validator import write_build_report


class EnrichedGitaSqliteBuilderTest(unittest.TestCase):
    def test_loads_enriched_gita_jsonl_into_sqlite_document_shape(self):
        documents = _load_gita_documents()

        verse = next(document for document in documents if document["id"] == "BG-2-47")

        self.assertEqual("gita", verse["collection"])
        self.assertEqual("Bhagavad Gita", verse["work"])
        self.assertEqual("Bhagavad Gita 2.47", verse["citation"])
        self.assertEqual("besant_wikisource_1922", verse["source_id"])
        self.assertEqual("gita/02/047", verse["section_path"])
        self.assertIn("fruit of action", verse["text"])
        self.assertIn("fruits of action", verse["commentary"])
        self.assertIn("karma yoga", verse["themes_json"])
        self.assertIn('"chapter": 2', verse["metadata_json"])
        self.assertIn('"verse": 47', verse["metadata_json"])

    def test_index_only_build_report_uses_sqlite_counts(self):
        with TemporaryDirectory() as temp_dir:
            with patch("rag_builder.validator.indexes_dir", return_value=Path(temp_dir)):
                write_build_report(
                    gita_val={},
                    mbh_val={},
                    sqlite_info={"gita_documents": 700, "mahabharata_documents": 3929},
                    download_warnings=[],
                )

            report = json.loads((Path(temp_dir) / "build_report.json").read_text(encoding="utf-8"))

        self.assertEqual(700, report["gita_verses_imported"])
        self.assertEqual(3929, report["mahabharata_chunks_imported"])


if __name__ == "__main__":
    unittest.main()
