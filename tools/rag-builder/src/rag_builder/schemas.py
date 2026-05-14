from __future__ import annotations

from dataclasses import dataclass, field, asdict
from typing import Any


@dataclass
class GitaVerseRecord:
    id: str
    work: str
    collection: str
    chapter: int
    chapter_title: str
    verse: int
    source_id: str
    source_title: str
    language: str
    sanskrit: str
    transliteration: str
    translation: str
    commentary: str
    themes: list[str]
    entities: list[str]
    citation: str
    source_url: str
    license: str
    search_text: str

    def to_json_obj(self) -> dict[str, Any]:
        return asdict(self)


@dataclass
class MahabharataChunkRecord:
    id: str
    work: str
    collection: str
    parva_number: int
    parva_name: str
    section: str
    subsection: str
    paragraph_index: int
    source_id: str
    source_title: str
    language: str
    text: str
    themes: list[str]
    entities: list[str]
    citation: str
    source_url: str
    license: str
    contains_gita: bool
    search_text: str

    def to_json_obj(self) -> dict[str, Any]:
        return asdict(self)


@dataclass
class DocumentRow:
    id: str
    collection: str
    work: str
    title: str
    section_path: str
    citation: str
    source_id: str
    language: str
    text: str
    sanskrit: str
    transliteration: str
    translation: str
    commentary: str
    themes_json: str
    entities_json: str
    metadata_json: str
