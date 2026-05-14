from __future__ import annotations

import html
import json
import re
from pathlib import Path
from typing import Any

from bs4 import BeautifulSoup

from rag_builder.paths import gita_processed_dir, gita_raw_dir, load_yaml
from rag_builder.schemas import GitaVerseRecord
from rag_builder.source_registry import get_source
from rag_builder.text_cleaning import clean_plain_text, strip_html_noise

# Standard English chapter titles (discourse N = chapter N in this edition)
GITA_CHAPTER_TITLES: dict[int, str] = {
    1: "Arjuna Vishada Yoga",
    2: "Sankhya Yoga",
    3: "Karma Yoga",
    4: "Jnana Karma Sanyasa Yoga",
    5: "Karma Sanyasa Yoga",
    6: "Dhyana Yoga",
    7: "Jnana Vijnana Yoga",
    8: "Aksara Parabrahman Yoga",
    9: "Raja Vidya Raja Guhya Yoga",
    10: "Vibhuti Yoga",
    11: "Visvarupa Darsana Yoga",
    12: "Bhakti Yoga",
    13: "Kshetra Kshetrajna Vibhaga Yoga",
    14: "Gunatraya Vibhaga Yoga",
    15: "Purushottama Prapti Yoga",
    16: "Daivasura Sampad Vibhaga Yoga",
    17: "Shraddhatraya Vibhaga Yoga",
    18: "Moksha Sanyasa Yoga",
}

VERSE_NUM_RE = re.compile(r"\((\d+)\)\s*$")
# Wikisource [sic] tooltips sometimes break the closing ")" on the verse marker.
VERSE_NUM_RE_LOOSE = re.compile(r"\((\d+)\)?\s*$")
# Rare OCR typo: "32)" with missing "("
VERSE_NUM_RE_TRAILING = re.compile(r"(\d+)\)\s*$")
DEVANAGARI_BLOCK = re.compile(r"[\u0900-\u097F]")
_DEVANAGARI_DIGITS = str.maketrans("\u0966\u0967\u0968\u0969\u096a\u096b\u096c\u096d\u096e\u096f", "0123456789")
_DEVANAGARI_VERSE_MARK = re.compile(r"॥\s*([\u0966-\u096F]+)\s*॥")


def _infer_themes_entities(text: str, themes_cfg: dict) -> tuple[list[str], list[str]]:
    low = text.lower()
    themes: set[str] = set()
    for row in themes_cfg.get("keyword_themes", []):
        for kw in row.get("keywords", []):
            if kw.lower() in low:
                themes.update(row.get("themes", []))
                break
    ent = list(themes_cfg.get("default_entities_gita", ["Krishna", "Arjuna"]))
    return sorted(themes), ent


def _paired_devanagari_verse_number(p_tag) -> int | None:
    """Verse index from the nearest preceding Sanskrit block (authoritative when English float is wrong)."""
    cur = p_tag
    for _ in range(80):
        cur = cur.find_previous_sibling()
        if cur is None:
            return None
        lang_block = cur.select_one(".wst-lang[lang=sa], .wst-lang, [lang=sa]")
        if lang_block is None and (cur.get("lang") or "").lower() == "sa":
            lang_block = cur
        if lang_block is None:
            continue
        raw = lang_block.get_text("\n", strip=True)
        matches = list(_DEVANAGARI_VERSE_MARK.finditer(raw))
        if len(matches) != 1:
            continue
        digits = matches[0].group(1)
        return int(digits.translate(_DEVANAGARI_DIGITS))
    return None


def _verse_number_for_paragraph(p_tag, chapter: int) -> int | None:
    float_v = _verse_number_from_floatright(p_tag)
    dev_v = _paired_devanagari_verse_number(p_tag)
    if float_v is None:
        return dev_v
    if dev_v is None:
        return float_v
    if chapter == 17 and float_v == 20 and dev_v == 19:
        return 19
    if chapter == 18 and float_v == 15 and dev_v == 14:
        return 14
    return float_v


def _verse_number_from_floatright(p_tag) -> int | None:
    """Read verse index from the Besant Wikisource float-right marker on a verse paragraph."""
    fr = p_tag.select_one("span.wst-floatright")
    if fr is None:
        return None
    for el in fr.select("[title]"):
        title = html.unescape(el.get("title") or "")
        m = re.search(r"\((\d+)\)", title)
        if m:
            return int(m.group(1))
    blob = fr.get_text(" ", strip=True)
    if not blob:
        return None
    m = VERSE_NUM_RE.search(blob)
    if m:
        return int(m.group(1))
    m = VERSE_NUM_RE_LOOSE.search(blob)
    if m:
        return int(m.group(1))
    m = VERSE_NUM_RE_TRAILING.search(blob)
    if m:
        return int(m.group(1))
    return None


def _extract_sanskrit_for_paragraph(p_tag) -> str:
    """Walk previous siblings from the English verse paragraph for Devanagari blocks."""
    parts: list[str] = []
    cur = p_tag
    for _ in range(40):
        cur = cur.find_previous_sibling()
        if cur is None:
            break
        lang = (cur.get("lang") or "").lower()
        cls = " ".join(cur.get("class", []))
        if lang == "sa" or "wst-lang" in cls:
            txt = clean_plain_text(strip_html_noise(cur.get_text(" ", strip=True)))
            if DEVANAGARI_BLOCK.search(txt):
                parts.append(txt)
    return "\n".join(reversed(parts)).strip()


def parse_discourse_html(html: str, chapter: int, themes_cfg: dict) -> list[GitaVerseRecord]:
    soup = BeautifulSoup(html, "lxml")
    container = soup.select_one(".prp-pages-output") or soup.select_one(".mw-parser-output")
    if container is None:
        return []

    src = get_source("besant_wikisource_1922") or {}
    source_id = "besant_wikisource_1922"
    source_title = src.get("title", "Bhagavad-Gita, Annie Besant, 4th edition")
    source_url = src.get("source_url", "https://en.wikisource.org/wiki/Bhagavad-Gita_(Besant_4th)")
    license_txt = src.get(
        "license",
        "Public domain in the United States; verify jurisdiction before commercial distribution",
    )
    chapter_title = GITA_CHAPTER_TITLES.get(chapter, f"Chapter {chapter}")

    verses: list[GitaVerseRecord] = []
    for p in container.find_all("p"):
        verse_num = _verse_number_for_paragraph(p, chapter)
        if verse_num is None:
            continue
        clone = BeautifulSoup(str(p), "lxml")
        for sp in clone.select("span.wst-floatright"):
            sp.decompose()
        for sup in clone.select("sup.reference"):
            sup.decompose()
        translation = clean_plain_text(clone.get_text(" ", strip=True))
        if not translation:
            continue
        sanskrit = _extract_sanskrit_for_paragraph(p)
        themes, entities = _infer_themes_entities(translation + " " + sanskrit, themes_cfg)
        vid = f"gita_{chapter:02d}_{verse_num:03d}_besant"
        citation = f"Bhagavad Gita {chapter}.{verse_num}"
        search_bits = [citation, translation, sanskrit, chapter_title] + themes + entities
        search_text = clean_plain_text(" ".join(search_bits))
        verses.append(
            GitaVerseRecord(
                id=vid,
                work="Bhagavad Gita",
                collection="gita",
                chapter=chapter,
                chapter_title=chapter_title,
                verse=verse_num,
                source_id=source_id,
                source_title=source_title,
                language="en",
                sanskrit=sanskrit,
                transliteration="",
                translation=translation,
                commentary="",
                themes=themes,
                entities=entities,
                citation=citation,
                source_url=source_url,
                license=license_txt,
                search_text=search_text,
            )
        )
    verses.sort(key=lambda v: v.verse)
    return verses


def normalize_gita_from_raw(raw_dir: Path, themes_cfg: dict) -> dict[str, Any]:
    """Read discourse_01..18.html; write jsonl + chapters + validation."""
    processed = gita_processed_dir()
    processed.mkdir(parents=True, exist_ok=True)
    all_verses: list[GitaVerseRecord] = []
    per_chapter: dict[int, list[GitaVerseRecord]] = {i: [] for i in range(1, 19)}
    warnings: list[str] = []

    for n in range(1, 19):
        fn = raw_dir / f"discourse_{n:02d}.html"
        if not fn.exists():
            warnings.append(f"Missing Besant raw file: {fn.name} — run download or save HTML manually.")
            continue
        html = fn.read_text(encoding="utf-8", errors="replace")
        parsed = parse_discourse_html(html, n, themes_cfg)
        if not parsed:
            warnings.append(f"No verses parsed for discourse {n} ({fn.name}).")
        for v in parsed:
            per_chapter[n].append(v)
        all_verses.extend(parsed)

    # Deduplicate by (chapter, verse) keeping first
    seen: set[tuple[int, int]] = set()
    deduped: list[GitaVerseRecord] = []
    for v in sorted(all_verses, key=lambda x: (x.chapter, x.verse)):
        key = (v.chapter, v.verse)
        if key in seen:
            warnings.append(f"Duplicate verse dropped: {key}")
            continue
        seen.add(key)
        deduped.append(v)

    before_canon = len(deduped)
    deduped = [v for v in deduped if not (v.chapter == 13 and v.verse == 35)]
    if len(deduped) < before_canon:
        warnings.append(
            "Omitted Besant Bhagavad Gita 13.35 from export: the 700-shloka canon counts 34 verses in chapter 13; "
            "Besant Wikisource labels a 35th stanza."
        )

    per_chapter = {i: [] for i in range(1, 19)}
    for v in deduped:
        per_chapter[v.chapter].append(v)

    out_jsonl = processed / "gita_verses.jsonl"
    with out_jsonl.open("w", encoding="utf-8") as f:
        for v in deduped:
            f.write(json.dumps(v.to_json_obj(), ensure_ascii=False) + "\n")

    chapters_out: list[dict[str, Any]] = []
    for ch in range(1, 19):
        vs = per_chapter[ch]
        vcount = len({v.verse for v in vs})
        tset: set[str] = set()
        for v in vs:
            tset.update(v.themes)
        chapters_out.append(
            {
                "chapter": ch,
                "title_sanskrit": GITA_CHAPTER_TITLES.get(ch, ""),
                "title_english": GITA_CHAPTER_TITLES.get(ch, ""),
                "verse_count_verified": vcount,
                "themes": sorted(tset),
            }
        )

    (processed / "gita_chapters.json").write_text(
        json.dumps(chapters_out, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )

    expected = 700
    report = {
        "verse_count": len(deduped),
        "expected_verse_count": expected,
        "complete": len(deduped) == expected,
        "chapters_with_zero_verses": [ch for ch in range(1, 19) if not per_chapter[ch]],
        "warnings": warnings,
    }
    (processed / "gita_validation_report.json").write_text(
        json.dumps(report, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    return report


def write_gita_concepts_jsonl(processed_dir: Path) -> None:
    """Curated concepts (static); related_verse ids are logical (chapter.verse) prefixes."""
    concepts = [
        {
            "id": "concept_karma_yoga",
            "name": "Karma Yoga",
            "aliases": ["selfless action", "duty without attachment"],
            "description": "Yoga of selfless action: performing one's duty with steadiness while releasing obsession with outcomes.",
            "related_verses": ["gita_02_047", "gita_03_019", "gita_18_048"],
            "search_terms": ["action", "duty", "fruits of action", "detachment", "motive"],
        },
        {
            "id": "concept_bhakti_yoga",
            "name": "Bhakti Yoga",
            "aliases": ["path of devotion", "loving offering"],
            "description": "Union through devotion: orienting the heart toward the Divine in love, trust, and humble service.",
            "related_verses": ["gita_09_026", "gita_12_006", "gita_18_065"],
            "search_terms": ["devotion", "worship", "faith", "love"],
        },
        {
            "id": "concept_jnana_yoga",
            "name": "Jnana Yoga",
            "aliases": ["yoga of knowledge", "discernment"],
            "description": "The path of insight: discrimination between the real and the unreal, and steady contemplation.",
            "related_verses": ["gita_04_038", "gita_13_001", "gita_18_066"],
            "search_terms": ["knowledge", "wisdom", "discern", "see clearly"],
        },
        {
            "id": "concept_dharma",
            "name": "Dharma",
            "aliases": ["righteousness", "sacred duty", "the way of integrity"],
            "description": "The ordering principle of right action, truthfulness, and responsibility fitted to one's station and conscience.",
            "related_verses": ["gita_02_031", "gita_03_008", "gita_18_066"],
            "search_terms": ["duty", "righteousness", "law", "integrity"],
        },
        {
            "id": "concept_detachment",
            "name": "Detachment (from fruits)",
            "aliases": ["non-attachment", "evenness"],
            "description": "Freedom from being possessed by results: caring deeply for action while not being enslaved by reward.",
            "related_verses": ["gita_02_047", "gita_02_048", "gita_05_012"],
            "search_terms": ["detachment", "fruits", "equanimity", "bondage"],
        },
        {
            "id": "concept_gunas",
            "name": "The Gunas",
            "aliases": ["modes of nature", "strands of prakriti"],
            "description": "The interplay of sattva, rajas, and tamas shaping mind, motive, and movement.",
            "related_verses": ["gita_14_005", "gita_14_019", "gita_18_040"],
            "search_terms": ["guna", "nature", "modes", "qualities"],
        },
        {
            "id": "concept_self_control",
            "name": "Self-mastery",
            "aliases": ["discipline of the senses", "self-control"],
            "description": "Training attention and desire so the mind becomes a steady instrument rather than a tyrant.",
            "related_verses": ["gita_02_058", "gita_03_006", "gita_06_035"],
            "search_terms": ["senses", "restraint", "steady", "mind"],
        },
        {
            "id": "concept_devotion",
            "name": "Devotion",
            "aliases": ["loving dedication"],
            "description": "Warm, personal turning of the heart toward the Divine in prayer, service, and remembrance.",
            "related_verses": ["gita_09_022", "gita_12_013", "gita_18_056"],
            "search_terms": ["devotion", "love", "offering"],
        },
        {
            "id": "concept_surrender",
            "name": "Surrender",
            "aliases": ["self-offering", "refuge"],
            "description": "Placing one's will gently into higher wisdom: not helplessness, but sacred trust.",
            "related_verses": ["gita_18_066", "gita_07_014", "gita_12_006"],
            "search_terms": ["surrender", "refuge", "trust"],
        },
        {
            "id": "concept_fear",
            "name": "Fear and trembling of the heart",
            "aliases": ["anxiety", "dread"],
            "description": "The shrinking of the heart before the unknown; met with breath, truth, and compassionate resolve.",
            "related_verses": ["gita_02_012", "gita_18_030"],
            "search_terms": ["fear", "tremble", "afraid"],
        },
        {
            "id": "concept_anger",
            "name": "Anger",
            "aliases": ["wrath", "burning heat"],
            "description": "A signal fire that can protect or destroy; the Gita teaches seeing its roots and cooling it with wisdom.",
            "related_verses": ["gita_02_062", "gita_16_021"],
            "search_terms": ["anger", "wrath", "passion"],
        },
        {
            "id": "concept_desire",
            "name": "Desire",
            "aliases": ["craving", "longing"],
            "description": "The movement of appetite and attachment; understood as a field for disciplined self-knowledge.",
            "related_verses": ["gita_03_037", "gita_03_039", "gita_16_010"],
            "search_terms": ["desire", "craving", "lust"],
        },
        {
            "id": "concept_duty",
            "name": "Duty (svadharma)",
            "aliases": ["right work", "one's own calling"],
            "description": "The shape of responsibility unique to one's situation and conscience, honored without pride or escape.",
            "related_verses": ["gita_03_035", "gita_18_047"],
            "search_terms": ["duty", "calling", "station"],
        },
        {
            "id": "concept_equanimity",
            "name": "Equanimity",
            "aliases": ["evenness of mind", "samatva"],
            "description": "A heart equally poised before honor and dishonor, gain and loss, heat and cold.",
            "related_verses": ["gita_02_048", "gita_12_013", "gita_18_010"],
            "search_terms": ["equal", "equanimity", "balanced"],
        },
        {
            "id": "concept_moksha",
            "name": "Moksha",
            "aliases": ["liberation", "freedom", "release"],
            "description": "Freedom from the small self's compulsions; abiding in the vast peace of what is real.",
            "related_verses": ["gita_02_072", "gita_05_028", "gita_18_055"],
            "search_terms": ["liberation", "freedom", "release", "peace"],
        },
    ]
    out = processed_dir / "gita_concepts.jsonl"
    with out.open("w", encoding="utf-8") as f:
        for c in concepts:
            f.write(json.dumps(c, ensure_ascii=False) + "\n")


def run_normalize_gita() -> dict[str, Any]:
    themes_cfg = load_yaml("themes.yaml")
    raw_dir = gita_raw_dir() / "besant_wikisource"
    rep = normalize_gita_from_raw(raw_dir, themes_cfg)
    write_gita_concepts_jsonl(gita_processed_dir())
    return rep
