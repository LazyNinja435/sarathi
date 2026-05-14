from __future__ import annotations

import re
import unicodedata


_WS = re.compile(r"[ \t\r\f\v]+")


def normalize_unicode(s: str) -> str:
    return unicodedata.normalize("NFC", s or "")


def strip_html_noise(text: str) -> str:
    t = normalize_unicode(text)
    t = re.sub(r"<[^>]+>", " ", t)
    t = _WS.sub(" ", t)
    return t.strip()


def clean_plain_text(text: str) -> str:
    t = normalize_unicode(text)
    t = t.replace("\u00a0", " ")
    t = re.sub(r"\[\d+\]", "", t)
    t = _WS.sub(" ", t)
    return t.strip()


def collapse_blank_lines(text: str) -> str:
    lines = [ln.rstrip() for ln in text.splitlines()]
    out: list[str] = []
    prev_blank = False
    for ln in lines:
        blank = ln.strip() == ""
        if blank and prev_blank:
            continue
        out.append(ln)
        prev_blank = blank
    return "\n".join(out).strip()
