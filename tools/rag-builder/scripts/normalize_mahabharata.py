#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
from rag_builder.logging_utils import setup_logging
from rag_builder.mahabharata_parser import normalize_mahabharata


def main() -> int:
    setup_logging()
    print(normalize_mahabharata())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
