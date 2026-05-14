#!/usr/bin/env python3
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
from rag_builder.logging_utils import setup_logging
from rag_builder.validator import validate_corpus


def main() -> int:
    setup_logging()
    v = validate_corpus()
    print(json.dumps(v, indent=2))
    return 1 if v.get("errors") else 0


if __name__ == "__main__":
    raise SystemExit(main())
