# Bhagavad Gita sources (Sarathi)

## Primary

**Annie Besant 1922 (Wikisource)** — `raw/besant_wikisource/`  
HTML from `action=parse` for pages `Bhagavad-Gita (Besant 4th)/Discourse 1` … `Discourse 18`. Verse numbers are extracted from rendered page layout.

## Secondary

**Swami Swarupananda (Sacred Texts)** — `raw/swarupananda_sacred_texts/`  
Reserved for manual drops if automated download fails (Cloudflare).

**Edwin Arnold (Gutenberg)** — `raw/arnold_gutenberg/`  
Optional poetic text; not used for strict verse indexing.

## Processed outputs

| File | Description |
|------|-------------|
| `processed/gita_verses.jsonl` | One verse per line (Besant primary). |
| `processed/gita_chapters.json` | Chapter metadata and theme hints. |
| `processed/gita_concepts.jsonl` | Curated concepts linked to verse ids. |
| `processed/gita_validation_report.json` | Parse counts vs 700-verse expectation. |
