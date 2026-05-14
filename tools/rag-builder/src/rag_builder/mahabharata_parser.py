from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any

from rag_builder.paths import load_yaml, mahabharata_processed_dir, mahabharata_raw_dir
from rag_builder.schemas import MahabharataChunkRecord
from rag_builder.source_registry import get_source
from rag_builder.text_cleaning import clean_plain_text, collapse_blank_lines

SECTION_RE = re.compile(r"^SECTION\s+(.+?)\s*$", re.MULTILINE)
BOOK_RE = re.compile(r"^BOOK\s+(\d+)\s*$", re.MULTILINE)
PARVA_NAME_RE = re.compile(r"^([A-Z][A-Z\-']+)\s+PARVA\s*$", re.MULTILINE)
ROMAN_ONLY = re.compile(r"^[IVXLCDM]+$", re.IGNORECASE)


def roman_to_int(roman: str) -> int | None:
    s = (roman or "").strip().upper()
    if not s:
        return None
    if not ROMAN_ONLY.match(s):
        return None
    values = [
        ("M", 1000),
        ("CM", 900),
        ("D", 500),
        ("CD", 400),
        ("C", 100),
        ("XC", 90),
        ("L", 50),
        ("XL", 40),
        ("X", 10),
        ("IX", 9),
        ("V", 5),
        ("IV", 4),
        ("I", 1),
    ]
    i = 0
    total = 0
    for sym, val in values:
        ln = len(sym)
        while s[i:].startswith(sym):
            total += val
            i += ln
    if i != len(s):
        return None
    return total


def parse_section_token(tok: str) -> tuple[str, int | None]:
    t = tok.strip()
    if t.isdigit():
        return t, int(t)
    ri = roman_to_int(t)
    return t, ri


def _slug(s: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", s.lower()).strip("_")


def _section_id_token(sec_label: str, sec_num: int | None) -> str:
    if sec_num is not None:
        return f"{sec_num:03d}"
    return _slug(sec_label) or "unk"


def _infer_contains_gita(parva_number: int, section_num: int | None) -> bool:
    if parva_number != 6 or section_num is None:
        return False
    return 23 <= section_num <= 40


def _infer_themes_entities(text: str, themes_cfg: dict) -> tuple[list[str], list[str]]:
    low = text.lower()
    themes: set[str] = set()
    for row in themes_cfg.get("keyword_themes", []):
        for kw in row.get("keywords", []):
            if kw.lower() in low:
                themes.update(row.get("themes", []))
                break
    ent = list(themes_cfg.get("default_entities_mahabharata", ["Krishna", "Arjuna"]))
    return sorted(themes), ent


def _chunk_body_text(
    body: str,
    min_chars: int,
    max_chars: int,
    merge_single_newlines: bool,
) -> list[str]:
    t = collapse_blank_lines(body)
    if merge_single_newlines:
        t = re.sub(r"(?<!\n)\n(?!\n)", " ", t)
    paras = [clean_plain_text(p) for p in re.split(r"\n\s*\n+", t) if clean_plain_text(p)]
    chunks: list[str] = []
    buf = ""
    for p in paras:
        if not buf:
            buf = p
        elif len(buf) + 1 + len(p) <= max_chars:
            buf = f"{buf}\n\n{p}"
        else:
            if len(buf) >= min_chars or not chunks:
                chunks.append(buf)
            else:
                chunks[-1] = f"{chunks[-1]}\n\n{buf}".strip()
            buf = p
    if buf:
        if len(buf) < min_chars and chunks:
            merged = f"{chunks[-1]}\n\n{buf}".strip()
            if len(merged) <= max_chars * 1.15:
                chunks[-1] = merged
            else:
                chunks.append(buf)
        else:
            chunks.append(buf)
    return [c for c in chunks if c]


def parse_mahabharata_file(
    path: Path,
    themes_cfg: dict,
    chunk_cfg: dict,
) -> tuple[list[MahabharataChunkRecord], dict[str, Any]]:
    text = path.read_text(encoding="utf-8", errors="replace")
    m_book = BOOK_RE.search(text)
    parva_number = int(m_book.group(1)) if m_book else int(path.stem.replace("maha", ""))
    m_parva = PARVA_NAME_RE.search(text[:8000])
    parva_name = (m_parva.group(1).strip().title() + " Parva") if m_parva else f"Parva {parva_number}"

    src = get_source("ganguli_aasi_github") or {}
    source_id = "ganguli_aasi_github"
    source_title = src.get(
        "title",
        "The Mahabharata of Krishna-Dwaipayana Vyasa, translated by K.M. Ganguli",
    )
    source_url = src.get("source_url", "https://github.com/aasi-archive/mbh")
    license_txt = src.get(
        "license",
        "Public domain in the United States; verify jurisdiction before commercial distribution",
    )

    min_chars = int(chunk_cfg.get("min_chars", 400))
    max_chars = int(chunk_cfg.get("max_chars", 3500))
    merge_nl = bool(chunk_cfg.get("paragraph_merge_single_newlines", True))
    parva_slug = _slug(parva_name.replace(" Parva", ""))

    records: list[MahabharataChunkRecord] = []
    meta_sections: list[dict[str, Any]] = []

    section_matches = list(SECTION_RE.finditer(text))
    for sec_index, m in enumerate(section_matches, start=1):
        start = m.end()
        sec_token_raw = m.group(1).strip()
        sec_label, sec_num = parse_section_token(sec_token_raw)
        next_m = section_matches[sec_index] if sec_index < len(section_matches) else None
        end = next_m.start() if next_m else len(text)
        section_body = text[start:end]
        lines = section_body.splitlines()
        subsection = ""
        body_start_idx = 0
        if lines and lines[0].strip().startswith("(") and lines[0].strip().endswith(")"):
            subsection = clean_plain_text(lines[0])
            body_start_idx = 1
        body = "\n".join(lines[body_start_idx:])
        chunks = _chunk_body_text(body, min_chars, max_chars, merge_nl)
        meta_sections.append(
            {"section_index": sec_index, "section_label": sec_label, "section_number": sec_num, "chunks": len(chunks)}
        )
        for idx, chunk_text in enumerate(chunks, start=1):
            themes, entities = _infer_themes_entities(chunk_text, themes_cfg)
            contains = _infer_contains_gita(parva_number, sec_num)
            sec_key = _section_id_token(sec_label, sec_num)
            cid = f"mahabharata_{parva_number:02d}_{parva_slug}_section_{sec_key}_occ_{sec_index:04d}_para_{idx:03d}_ganguli"
            citation = f"Mahabharata, {parva_name}, Section {sec_label}"
            search = clean_plain_text(
                " ".join(
                    [
                        "Mahabharata",
                        parva_name,
                        "Section",
                        sec_label,
                        chunk_text,
                        " ".join(themes),
                        " ".join(entities),
                    ]
                )
            )
            records.append(
                MahabharataChunkRecord(
                    id=cid,
                    work="Mahabharata",
                    collection="mahabharata",
                    parva_number=parva_number,
                    parva_name=parva_name,
                    section=sec_label,
                    subsection=subsection,
                    paragraph_index=idx,
                    source_id=source_id,
                    source_title=source_title,
                    language="en",
                    text=chunk_text,
                    themes=themes,
                    entities=entities,
                    citation=citation,
                    source_url=source_url,
                    license=license_txt,
                    contains_gita=contains,
                    search_text=search,
                )
            )

    summary = {
        "file": path.name,
        "parva_number": parva_number,
        "parva_name": parva_name,
        "sections_detected": len(meta_sections),
        "chunks": len(records),
    }
    return records, summary


def normalize_mahabharata() -> dict[str, Any]:
    themes_cfg = load_yaml("themes.yaml")
    chunk_cfg = load_yaml("chunking.yaml").get("mahabharata", {})
    max_parva = chunk_cfg.get("max_parva_number")
    if max_parva is not None:
        max_parva = int(max_parva)

    raw_dir = mahabharata_raw_dir() / "ganguli_aasi"
    processed = mahabharata_processed_dir()
    processed.mkdir(parents=True, exist_ok=True)

    warnings: list[str] = []
    all_recs: list[MahabharataChunkRecord] = []
    per_file_summaries: list[dict[str, Any]] = []

    files = sorted(raw_dir.glob("maha*.txt"))
    if not files:
        warnings.append(f"No maha*.txt files under {raw_dir}. Download AASI txt or place files manually.")

    for path in files:
        n = int(path.stem.replace("maha", ""))
        if max_parva is not None and n > max_parva:
            continue
        recs, summ = parse_mahabharata_file(path, themes_cfg, chunk_cfg)
        all_recs.extend(recs)
        per_file_summaries.append(summ)

    out_jsonl = processed / "mahabharata_chunks.jsonl"
    with out_jsonl.open("w", encoding="utf-8") as f:
        for r in all_recs:
            f.write(json.dumps(r.to_json_obj(), ensure_ascii=False) + "\n")

    parvas_meta: list[dict[str, Any]] = []
    for s in per_file_summaries:
        parvas_meta.append(
            {
                "parva_number": s["parva_number"],
                "parva_name": s["parva_name"],
                "section_count": s["sections_detected"],
                "chunk_count": s["chunks"],
                "source_coverage_status": "present_in_aasi_txt",
                "summary": "",
            }
        )

    expected = 18
    present = {s["parva_number"] for s in per_file_summaries}
    missing = [n for n in range(1, expected + 1) if n not in present]
    if missing:
        warnings.append(
            "Missing parva files vs expected 1..18: " + ", ".join(f"maha{n:02d}.txt" for n in missing)
        )

    (processed / "mahabharata_parvas.json").write_text(
        json.dumps(parvas_meta, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )

    val_rep = {
        "chunk_count": len(all_recs),
        "parvas_detected": len(per_file_summaries),
        "sections_detected": sum(s["sections_detected"] for s in per_file_summaries),
        "missing_parva_numbers": missing,
        "warnings": warnings,
    }
    (processed / "mahabharata_validation_report.json").write_text(
        json.dumps(val_rep, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    return val_rep
