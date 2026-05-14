#!/usr/bin/env python3
"""Verify sarathi-latest.json: APK hash, chunk hashes, reconstructed model SHA."""

from __future__ import annotations

import argparse
import hashlib
import json
import tempfile
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
    manifest_path = dist_dir / "sarathi-latest.json"
    if not manifest_path.is_file():
        raise SystemExit(f"Missing manifest: {manifest_path}")

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    app = manifest["app"]
    model = manifest["model"]

    apk = dist_dir / app["apkFileName"]
    if not apk.is_file():
        raise SystemExit(f"Missing APK: {apk}")
    apk_sha = sha256_file(apk)
    if apk_sha.lower() != str(app["apkSha256"]).lower():
        raise SystemExit(f"APK SHA mismatch. expected={app['apkSha256']} actual={apk_sha}")
    if apk.stat().st_size != int(app["apkSizeBytes"]):
        raise SystemExit("APK size mismatch")

    chunks = sorted(model["chunks"], key=lambda c: int(c["index"]))
    for c in chunks:
        part = dist_dir / c["fileName"]
        if not part.is_file():
            raise SystemExit(f"Missing chunk file: {part}")
        h = sha256_file(part)
        if h.lower() != str(c["sha256"]).lower():
            raise SystemExit(f"Chunk SHA mismatch for {part.name}: expected {c['sha256']} actual {h}")
        if part.stat().st_size != int(c["sizeBytes"]):
            raise SystemExit(f"Chunk size mismatch for {part.name}")

    with tempfile.TemporaryDirectory() as td:
        tmp = Path(td) / model["fileName"]
        with tmp.open("wb") as out:
            for c in chunks:
                part = dist_dir / c["fileName"]
                out.write(part.read_bytes())
        full_sha = sha256_file(tmp)
        if full_sha.lower() != str(model["sha256"]).lower():
            raise SystemExit(f"Full model SHA mismatch after reconstruction: expected {model['sha256']} actual {full_sha}")
        if tmp.stat().st_size != int(model["sizeBytes"]):
            raise SystemExit("Reconstructed model size mismatch")

    checksums = dist_dir / "checksums.sha256"
    if checksums.is_file():
        print(f"checksums.sha256 present ({checksums.stat().st_size} bytes)")

    print("Verification OK: APK, chunks, and reconstructed model match manifest.")


if __name__ == "__main__":
    main()
