#!/usr/bin/env python3
"""Verify sarathi-latest.json (app) or model-latest.json (model-only): hashes and optional chunk assembly."""

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


def verify_app_dist(dist_dir: Path) -> None:
    manifest_path = dist_dir / "sarathi-latest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    app = manifest["app"]
    model = manifest["model"]
    release = manifest.get("release", {})
    release_type = str(release.get("releaseType", "FULL_MODEL")).upper()

    apk = dist_dir / app["apkFileName"]
    if not apk.is_file():
        raise SystemExit(f"Missing APK: {apk}")
    apk_sha = sha256_file(apk)
    if apk_sha.lower() != str(app["apkSha256"]).lower():
        raise SystemExit(f"APK SHA mismatch. expected={app['apkSha256']} actual={apk_sha}")
    if apk.stat().st_size != int(app["apkSizeBytes"]):
        raise SystemExit("APK size mismatch")

    chunks = model.get("chunks") or []
    if release_type == "APP_ONLY" or not chunks:
        if release_type == "FULL_MODEL" and not chunks:
            raise SystemExit("FULL_MODEL manifest must include non-empty model.chunks")
        checksums = dist_dir / "checksums.sha256"
        if checksums.is_file():
            print(f"checksums.sha256 present ({checksums.stat().st_size} bytes)")
        print("Verification OK: APP_ONLY manifest and APK match.")
        return

    chunks = sorted(chunks, key=lambda c: int(c["index"]))
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


def verify_model_only_dist(dist_dir: Path) -> None:
    manifest_path = dist_dir / "model-latest.json"
    if not manifest_path.is_file():
        raise SystemExit(f"Missing manifest: {manifest_path}")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if "app" in manifest:
        raise SystemExit("model-only verify: unexpected app section in model-latest.json")
    model = manifest["model"]
    chunks = model.get("chunks") or []
    if not chunks:
        raise SystemExit("MODEL_ONLY manifest must include non-empty model.chunks")

    chunks = sorted(chunks, key=lambda c: int(c["index"]))
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

    print("Verification OK: model-only manifest, chunks, and reconstructed model match.")


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--dist-dir", type=Path, required=True)
    args = p.parse_args()
    dist_dir: Path = args.dist_dir
    if not dist_dir.is_dir():
        raise SystemExit(f"dist dir missing: {dist_dir}")

    app_manifest = dist_dir / "sarathi-latest.json"
    model_manifest = dist_dir / "model-latest.json"

    if app_manifest.is_file():
        verify_app_dist(dist_dir)
    elif model_manifest.is_file():
        verify_model_only_dist(dist_dir)
    else:
        raise SystemExit(f"Missing {app_manifest.name} or {model_manifest.name} in {dist_dir}")


if __name__ == "__main__":
    main()
