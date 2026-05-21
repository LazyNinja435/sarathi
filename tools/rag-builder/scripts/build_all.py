#!/usr/bin/env python3
"""Orchestrate download → normalize → SQLite index → validation reports."""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = ROOT.parents[1]
sys.path.insert(0, str(ROOT / "src"))

from rag_builder.downloader import run_downloads
from rag_builder.gita_parser import run_normalize_gita
from rag_builder.logging_utils import setup_logging
from rag_builder.mahabharata_parser import normalize_mahabharata
from rag_builder.paths import env_int, indexes_dir, load_yaml
from rag_builder.sqlite_builder import build_sqlite
from rag_builder.validator import validate_corpus, write_build_report


def main() -> int:
    p = argparse.ArgumentParser(description="Sarathi RAG offline builder")
    p.add_argument("--download", action="store_true", help="Fetch remote raw sources (best-effort)")
    p.add_argument("--normalize", action="store_true", help="Parse raw → processed JSONL")
    p.add_argument("--index", action="store_true", help="Build canonical SQLite and refresh Android/web RAG assets")
    p.add_argument("--verbose", action="store_true")
    args = p.parse_args()
    setup_logging(args.verbose)

    if not (args.download or args.normalize or args.index):
        p.print_help()
        print("\nNo actions specified. Typical full build:\n  python tools/rag-builder/scripts/build_all.py --download --normalize --index\n")
        return 0

    dl_warnings: list[str] = []
    chunk = load_yaml("chunking.yaml").get("mahabharata", {})
    parva_max = chunk.get("max_parva_number")
    if parva_max is not None:
        parva_max = int(parva_max)
    env_override = env_int("SARATHI_MBH_MAX_PARVA")
    if env_override is not None:
        parva_max = env_override

    if args.download:
        rep = run_downloads(parva_max=parva_max)
        for k in ("besant", "aasi", "arnold", "sacred_sbg", "sacred_maha"):
            dl_warnings.extend(rep.get(k) or [])

    gita_report: dict = {}
    mbh_report: dict = {}
    if args.normalize:
        gita_report = run_normalize_gita()
        mbh_report = normalize_mahabharata()

    sqlite_info: dict = {}
    if args.index:
        sqlite_info = build_sqlite()
        val = validate_corpus()
        write_build_report(gita_report, mbh_report, sqlite_info, dl_warnings)
        val_path = indexes_dir() / "validation_summary.json"
        val_path.write_text(json.dumps(val, indent=2), encoding="utf-8")
        if val.get("errors"):
            print("VALIDATION ERRORS:\n", "\n".join(val["errors"]))
            return 1
    elif args.normalize:
        sqlite_info = {"skipped": True}
        write_build_report(gita_report, mbh_report, sqlite_info, dl_warnings)

    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
