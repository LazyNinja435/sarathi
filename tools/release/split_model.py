#!/usr/bin/env python3
"""Split a large .litertlm into fixed-size chunks (<2 GiB each) for GitHub Release assets."""

from __future__ import annotations

import argparse
from pathlib import Path

DEFAULT_CHUNK_BYTES = 900 * 1024 * 1024  # 943_718_400


def main() -> None:
    p = argparse.ArgumentParser(description="Split model file into numbered .partNNN chunks.")
    p.add_argument("--input", required=True, type=Path, help="Path to source .litertlm")
    p.add_argument("--output-dir", required=True, type=Path, help="Directory for chunk files")
    p.add_argument("--chunk-bytes", type=int, default=DEFAULT_CHUNK_BYTES, help="Chunk size in bytes")
    args = p.parse_args()

    src: Path = args.input
    out_dir: Path = args.output_dir
    chunk_bytes: int = args.chunk_bytes
    if chunk_bytes <= 0:
        raise SystemExit("chunk-bytes must be positive")
    if not src.is_file():
        raise SystemExit(f"Input not found: {src}")
    out_dir.mkdir(parents=True, exist_ok=True)

    total = src.stat().st_size
    written_parts = 0
    with src.open("rb") as fin:
        part_idx = 1
        while True:
            buf = fin.read(chunk_bytes)
            if not buf:
                break
            name = f"{src.name}.part{part_idx:03d}"
            dest = out_dir / name
            dest.write_bytes(buf)
            print(f"Wrote {name} ({len(buf)} bytes)")
            written_parts += 1
            part_idx += 1

    if written_parts == 0:
        raise SystemExit("No data read from input (empty file?).")

    if sum(p.stat().st_size for p in out_dir.glob(f"{src.name}.part*")) != total:
        raise SystemExit("Chunk size accounting mismatch vs source file.")

    print(f"Done. Source {total} bytes -> {written_parts} chunk(s).")


if __name__ == "__main__":
    main()
