#!/usr/bin/env python3
"""Create sarathi-latest.json for GitHub Releases (APK + chunked model metadata)."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Any


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def load_version_props(repo_root: Path) -> tuple[int, str]:
    props_path = repo_root / "sarathi-version.properties"
    if not props_path.is_file():
        raise SystemExit(f"Missing {props_path}")
    data: dict[str, str] = {}
    for line in props_path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        data[k.strip()] = v.strip()
    code = int(data["SARATHI_VERSION_CODE"])
    name = data["SARATHI_VERSION_NAME"]
    return code, name


def discover_chunks(dist_dir: Path, base_name: str) -> list[Path]:
    pat = re.compile(rf"^{re.escape(base_name)}\.part(\d{{3}})$")
    found: list[tuple[int, Path]] = []
    for p in dist_dir.iterdir():
        if not p.is_file():
            continue
        m = pat.match(p.name)
        if m:
            found.append((int(m.group(1)), p))
    found.sort(key=lambda t: t[0])
    if not found:
        raise SystemExit(f"No chunk files matching {base_name}.partNNN in {dist_dir}")
    # Ensure contiguous indices starting at 1
    for i, (idx, _) in enumerate(found, start=1):
        if idx != i:
            raise SystemExit(f"Chunk indices must be contiguous starting at 1; saw index {idx} at position {i}")
    return [p for _, p in found]


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--repo-root", type=Path, required=True)
    p.add_argument("--dist-dir", type=Path, required=True)
    p.add_argument("--model-source", type=Path, required=True, help="Original monolithic model for full SHA + size")
    p.add_argument("--apk-name", type=str, default="", help="Override APK filename in dist (default sarathi-v{version}.apk)")
    p.add_argument("--repo", type=str, default="LazyNinja435/sarathi")
    p.add_argument("--tag", type=str, default="")
    args = p.parse_args()

    repo_root: Path = args.repo_root
    dist_dir: Path = args.dist_dir
    model_src: Path = args.model_source
    dist_dir.mkdir(parents=True, exist_ok=True)

    if not model_src.is_file():
        raise SystemExit(f"Model source not found: {model_src}")

    version_code, version_name = load_version_props(repo_root)
    tag = args.tag or f"v{version_name}"
    apk_name = args.apk_name or f"sarathi-v{version_name}.apk"
    apk_path = dist_dir / apk_name
    if not apk_path.is_file():
        raise SystemExit(f"APK not found at {apk_path}")

    model_file_name = model_src.name
    model_size = model_src.stat().st_size
    model_sha = sha256_file(model_src)

    chunk_paths = discover_chunks(dist_dir, model_file_name)
    chunk_size_bytes = 900 * 1024 * 1024
    chunks: list[dict[str, Any]] = []
    for i, cp in enumerate(chunk_paths, start=1):
        chunks.append(
            {
                "index": i,
                "fileName": cp.name,
                "sizeBytes": cp.stat().st_size,
                "sha256": sha256_file(cp),
            }
        )

    manifest: dict[str, Any] = {
        "app": {
            "packageName": "com.sarathi.app",
            "versionCode": version_code,
            "versionName": version_name,
            "apkFileName": apk_name,
            "apkSha256": sha256_file(apk_path),
            "apkSizeBytes": apk_path.stat().st_size,
        },
        "model": {
            "id": "gemma-4-e2b-it-litertlm",
            "fileName": model_file_name,
            "sizeBytes": model_size,
            "sha256": model_sha,
            "chunkSizeBytes": chunk_size_bytes,
            "chunks": chunks,
        },
        "release": {
            "repo": args.repo,
            "tag": tag,
            "manifestUrl": f"https://github.com/{args.repo}/releases/latest/download/sarathi-latest.json",
        },
    }

    out = dist_dir / "sarathi-latest.json"
    out.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {out}")


if __name__ == "__main__":
    main()
