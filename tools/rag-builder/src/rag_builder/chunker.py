"""Paragraph chunking helpers (reserved for shared use across works)."""

from __future__ import annotations


def merge_paragraphs(parts: list[str], min_chars: int, max_chars: int) -> list[str]:
    """Greedy merge of consecutive short strings."""
    out: list[str] = []
    buf = ""
    for p in parts:
        p = p.strip()
        if not p:
            continue
        if not buf:
            buf = p
        elif len(buf) + 2 + len(p) <= max_chars:
            buf = f"{buf}\n\n{p}"
        else:
            out.append(buf)
            buf = p
    if buf:
        out.append(buf)
    if out and len(out[-1]) < min_chars and len(out) > 1:
        merged = out[-2] + "\n\n" + out[-1]
        if len(merged) <= max_chars:
            out = out[:-2] + [merged]
    return out
