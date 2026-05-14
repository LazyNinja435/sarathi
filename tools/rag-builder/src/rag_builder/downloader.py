from __future__ import annotations

import logging
import time
from pathlib import Path

import requests
import yaml

from rag_builder.paths import (
    config_dir,
    gita_raw_dir,
    mahabharata_raw_dir,
    repo_root,
)

LOG = logging.getLogger(__name__)


def _session() -> requests.Session:
    s = requests.Session()
    cfg = yaml.safe_load((config_dir() / "sources.yaml").read_text(encoding="utf-8"))
    ua = (cfg.get("sacred_texts") or {}).get("user_agent") or "SarathiRagBuilder/1.0"
    s.headers.update({"User-Agent": ua})
    return s


def download_besant_discourses(session: requests.Session, out_dir: Path) -> list[str]:
    """Save rendered HTML from Wikisource parse API (expanded transclusion)."""
    out_dir.mkdir(parents=True, exist_ok=True)
    cfg = yaml.safe_load((config_dir() / "sources.yaml").read_text(encoding="utf-8"))
    api = cfg["wikisource"]["parse_api"]
    base = cfg["wikisource"]["besant_base_title"]
    warnings: list[str] = []
    for n in range(1, 19):
        title = f"{base}/Discourse {n}"
        params = {"action": "parse", "page": title, "prop": "text", "format": "json"}
        html = None
        for attempt in range(5):
            if attempt > 0:
                time.sleep(8 * attempt)
            else:
                time.sleep(1.5)
            r = session.get(api, params=params, timeout=120)
            if r.status_code == 429:
                warnings.append(f"Besant discourse {n}: HTTP 429 (retry {attempt + 1}/5)")
                continue
            if r.status_code != 200:
                warnings.append(f"Besant discourse {n}: HTTP {r.status_code}")
                break
            data = r.json()
            if "error" in data:
                warnings.append(f"Besant discourse {n}: API error {data['error']}")
                break
            html = data["parse"]["text"]["*"]
            break
        if html is None:
            warnings.append(f"Besant discourse {n}: failed after retries")
            continue
        fn = out_dir / f"discourse_{n:02d}.html"
        fn.write_text(html, encoding="utf-8")
        LOG.info("Saved %s", fn.relative_to(repo_root()))
    return warnings


def download_aasi_mahabharata(session: requests.Session, out_dir: Path, parva_max: int | None) -> list[str]:
    out_dir.mkdir(parents=True, exist_ok=True)
    cfg = yaml.safe_load((config_dir() / "sources.yaml").read_text(encoding="utf-8"))
    base = cfg["aasi_mahabharata"]["base_url"]
    pmin = int(cfg["aasi_mahabharata"]["parva_min"])
    pmax = int(cfg["aasi_mahabharata"]["parva_max"])
    if parva_max is not None:
        pmax = min(pmax, parva_max)
    warnings: list[str] = []
    for p in range(pmin, pmax + 1):
        name = f"maha{p:02d}.txt"
        url = base + name
        try:
            r = session.get(url, timeout=300)
            if r.status_code != 200:
                warnings.append(f"{name}: HTTP {r.status_code}")
                continue
            (out_dir / name).write_bytes(r.content)
            LOG.info("Saved %s (%d bytes)", name, len(r.content))
        except requests.RequestException as e:
            warnings.append(f"{name}: {e}")
    return warnings


def download_gutenberg_arnold(session: requests.Session, out_dir: Path) -> list[str]:
    out_dir.mkdir(parents=True, exist_ok=True)
    cfg = yaml.safe_load((config_dir() / "sources.yaml").read_text(encoding="utf-8"))
    url = cfg["gutenberg"]["arnold"]["mirror_txt"]
    warnings: list[str] = []
    try:
        r = session.get(url, timeout=120)
        if r.status_code != 200:
            warnings.append(f"Gutenberg Arnold: HTTP {r.status_code}")
            return warnings
        (out_dir / "pg2388.txt").write_bytes(r.content)
        LOG.info("Saved pg2388.txt (%d bytes)", len(r.content))
    except requests.RequestException as e:
        warnings.append(f"Gutenberg Arnold: {e}")
    return warnings


def download_sacred_texts_index(session: requests.Session, out_dir: Path, label: str, url: str) -> list[str]:
    """Best-effort; may fail behind Cloudflare."""
    out_dir.mkdir(parents=True, exist_ok=True)
    warnings: list[str] = []
    try:
        r = session.get(url, timeout=60)
        if r.status_code != 200:
            warnings.append(f"{label}: HTTP {r.status_code}")
            return warnings
        (out_dir / "index.html").write_bytes(r.content)
        LOG.info("Saved sacred-texts index for %s", label)
    except requests.RequestException as e:
        warnings.append(f"{label}: sacred-texts fetch failed ({e}). Place HTML manually under {out_dir}")
    return warnings


def run_downloads(parva_max: int | None = None) -> dict:
    session = _session()
    report: dict = {"warnings": [], "ok": True}
    besant_dir = gita_raw_dir() / "besant_wikisource"
    report["besant"] = download_besant_discourses(session, besant_dir)
    report["warnings"].extend(report["besant"])

    aasi_dir = mahabharata_raw_dir() / "ganguli_aasi"
    report["aasi"] = download_aasi_mahabharata(session, aasi_dir, parva_max)
    report["warnings"].extend(report["aasi"])

    arnold_dir = gita_raw_dir() / "arnold_gutenberg"
    report["arnold"] = download_gutenberg_arnold(session, arnold_dir)
    report["warnings"].extend(report["arnold"])

    cfg = yaml.safe_load((config_dir() / "sources.yaml").read_text(encoding="utf-8"))
    st = cfg.get("sacred_texts") or {}
    report["sacred_sbg"] = download_sacred_texts_index(
        session,
        gita_raw_dir() / "swarupananda_sacred_texts",
        "swarupananda_index",
        st.get("swarupananda_index", ""),
    )
    report["warnings"].extend(report["sacred_sbg"])
    report["sacred_maha"] = download_sacred_texts_index(
        session,
        mahabharata_raw_dir() / "ganguli_sacred_texts",
        "ganguli_maha_index",
        st.get("ganguli_maha_index", ""),
    )
    report["warnings"].extend(report["sacred_maha"])

    if any(report[k] for k in ("besant", "aasi", "arnold") if report.get(k)):
        # individual lists may have warnings; still ok if primary succeeded partially
        pass
    return report
