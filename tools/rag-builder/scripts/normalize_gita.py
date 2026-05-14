#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
from rag_builder.gita_parser import run_normalize_gita
from rag_builder.logging_utils import setup_logging


def main() -> int:
    setup_logging()
    print(run_normalize_gita())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
