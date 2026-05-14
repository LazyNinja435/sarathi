#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
from rag_builder.downloader import run_downloads
from rag_builder.logging_utils import setup_logging
from rag_builder.paths import env_int, load_yaml


def main() -> int:
    setup_logging()
    chunk = load_yaml("chunking.yaml").get("mahabharata", {})
    pm = chunk.get("max_parva_number")
    if pm is not None:
        pm = int(pm)
    eo = env_int("SARATHI_MBH_MAX_PARVA")
    if eo is not None:
        pm = eo
    rep = run_downloads(parva_max=pm)
    for w in rep.get("warnings", []):
        print("WARN:", w)
    for k, v in rep.items():
        if k != "warnings" and v:
            for x in v:
                print(f"{k}:", x)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
