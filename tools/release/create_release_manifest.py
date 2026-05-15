#!/usr/bin/env python3
"""Create sarathi-latest.json or model-latest.json for GitHub Releases."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Any

DEFAULT_EXTERNAL_MODEL_MANIFEST_URL = (
    "https://github.com/LazyNinja435/sarathi/releases/download/model-gemma-4-e2b/model-latest.json"
)


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
    for i, (idx, _) in enumerate(found, start=1):
        if idx != i:
            raise SystemExit(f"Chunk indices must be contiguous starting at 1; saw index {idx} at position {i}")
    return [p for _, p in found]


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--repo-root", type=Path, required=True)
    p.add_argument("--dist-dir", type=Path, required=True)
    p.add_argument("--model-source", type=Path, required=True, help="Monolithic model for metadata (SHA + size)")
    p.add_argument("--apk-name", type=str, default="", help="Override APK filename in dist (default sarathi-v{version}.apk)")
    p.add_argument("--repo", type=str, default="LazyNinja435/sarathi")
    p.add_argument("--tag", type=str, default="")
    p.add_argument(
        "--release-type",
        choices=("app_only", "full_model", "model_only"),
        default="app_only",
        help="app_only: APK manifest + modelSource URL, no chunks; full_model: inline chunks; model_only: model-latest.json",
    )
    p.add_argument("--model-version", type=str, default="2026.04")
    p.add_argument("--requires-model-update", action="store_true", help="Set app.requiresModelUpdate true (publisher opt-in)")
    p.add_argument(
        "--external-model-manifest-url",
        type=str,
        default=DEFAULT_EXTERNAL_MODEL_MANIFEST_URL,
        help="App manifest modelSource.externalManifestUrl (APP_ONLY)",
    )
    p.add_argument(
        "--model-release-tag",
        type=str,
        default="model-gemma-4-e2b",
        help="Git tag for MODEL_ONLY release (manifestUrl + chunk downloads)",
    )
    args = p.parse_args()

    repo_root: Path = args.repo_root
    dist_dir: Path = args.dist_dir
    model_src: Path = args.model_source
    dist_dir.mkdir(parents=True, exist_ok=True)

    if not model_src.is_file():
        raise SystemExit(f"Model source not found: {model_src}")

    model_file_name = model_src.name
    model_size = model_src.stat().st_size
    model_sha = sha256_file(model_src)
    chunk_size_bytes = 943_718_400
    model_id = "gemma-4-e2b-it-litertlm"

    if args.release_type == "model_only":
        tag = args.model_release_tag
        manifest_url = f"https://github.com/{args.repo}/releases/download/{tag}/model-latest.json"
        chunk_paths = discover_chunks(dist_dir, model_file_name)
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
            "schemaVersion": 1,
            "release": {
                "repo": args.repo,
                "tag": tag,
                "releaseType": "MODEL_ONLY",
                "manifestUrl": manifest_url,
                "publishedAt": None,
            },
            "model": {
                "required": False,
                "updatePolicy": "DOWNLOAD_IF_MISSING_OR_SHA_MISMATCH",
                "id": model_id,
                "version": args.model_version,
                "fileName": model_file_name,
                "sizeBytes": model_size,
                "sha256": model_sha,
                "chunkSizeBytes": chunk_size_bytes,
                "chunks": chunks,
            },
        }
        out = dist_dir / "model-latest.json"
        out.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
        print(f"Wrote {out} (MODEL_ONLY)")
        return

    version_code, version_name = load_version_props(repo_root)
    tag = args.tag or f"v{version_name}"
    apk_name = args.apk_name or f"sarathi-v{version_name}.apk"
    apk_path = dist_dir / apk_name
    if not apk_path.is_file():
        raise SystemExit(f"APK not found at {apk_path}")

    release_type = "APP_ONLY" if args.release_type == "app_only" else "FULL_MODEL"
    chunks = []

    if args.release_type == "full_model":
        chunk_paths = discover_chunks(dist_dir, model_file_name)
        for i, cp in enumerate(chunk_paths, start=1):
            chunks.append(
                {
                    "index": i,
                    "fileName": cp.name,
                    "sizeBytes": cp.stat().st_size,
                    "sha256": sha256_file(cp),
                }
            )

    if release_type == "APP_ONLY":
        model_obj: dict[str, Any] = {
            "required": False,
            "updatePolicy": "KEEP_EXISTING_IF_COMPATIBLE",
            "id": model_id,
            "version": args.model_version,
            "fileName": model_file_name,
            "sizeBytes": model_size,
            "sha256": model_sha,
            "chunkSizeBytes": chunk_size_bytes,
            "chunks": [],
        }
        app_requires_model = False
        model_source: dict[str, Any] = {
            "mode": "INLINE_OR_EXTERNAL",
            "externalManifestUrl": args.external_model_manifest_url,
        }
    else:
        model_obj = {
            "required": True,
            "updatePolicy": "DOWNLOAD_IF_MISSING_OR_SHA_MISMATCH",
            "id": model_id,
            "version": args.model_version,
            "fileName": model_file_name,
            "sizeBytes": model_size,
            "sha256": model_sha,
            "chunkSizeBytes": chunk_size_bytes,
            "chunks": chunks,
        }
        app_requires_model = bool(args.requires_model_update)
        model_source = None

    manifest = {
        "schemaVersion": 1,
        "release": {
            "repo": args.repo,
            "tag": tag,
            "releaseType": release_type,
            "manifestUrl": f"https://github.com/{args.repo}/releases/latest/download/sarathi-latest.json",
            "publishedAt": None,
        },
        "app": {
            "packageName": "com.sarathi.app",
            "versionCode": version_code,
            "versionName": version_name,
            "apkFileName": apk_name,
            "apkSha256": sha256_file(apk_path),
            "apkSizeBytes": apk_path.stat().st_size,
            "minSupportedModelId": model_id,
            "supportedModelIds": [model_id],
            "requiresModelUpdate": app_requires_model,
        },
        "model": model_obj,
    }
    if model_source is not None:
        manifest["modelSource"] = model_source

    out = dist_dir / "sarathi-latest.json"
    out.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {out} ({release_type})")


if __name__ == "__main__":
    main()
