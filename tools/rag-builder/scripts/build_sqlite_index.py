#!/usr/bin/env python3
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
from rag_builder.logging_utils import setup_logging
from rag_builder.sqlite_builder import build_sqlite


def main() -> int:
    setup_logging()
    print(json.dumps(build_sqlite(), indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
