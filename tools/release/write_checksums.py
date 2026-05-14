#!/usr/bin/env python3
"""Write checksums.sha256 (GNU-style) for all files in dist except the checksum file itself."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--dist-dir", type=Path, required=True)
    args = p.parse_args()
    dist_dir: Path = args.dist_dir
    if not dist_dir.is_dir():
        raise SystemExit(f"dist dir missing: {dist_dir}")
    files = sorted([p for p in dist_dir.iterdir() if p.is_file() and p.name != "checksums.sha256"])
    if not files:
        raise SystemExit("No files to checksum")
    lines: list[str] = []
    for f in files:
        digest = sha256_file(f)
        lines.append(f"{digest}  {f.name}")
    out = dist_dir / "checksums.sha256"
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {out} ({len(files)} files)")


if __name__ == "__main__":
    main()
