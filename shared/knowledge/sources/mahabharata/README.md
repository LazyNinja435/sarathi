# Mahabharata (Ganguli English) — Sarathi ingestion

## Practical mirror

**AASI GitHub** — `raw/ganguli_aasi/`  
Files `maha01.txt` … `maha18.txt` from [aasi-archive/mbh](https://github.com/aasi-archive/mbh) `txt/`. This is the default normalizer input.

## Sacred Texts

**sacred-texts.com** — `raw/ganguli_sacred_texts/`  
Optional HTML mirror; often blocked for scripted downloads. Use browser-exported HTML here if you need it.

## Coverage

The upstream repository may omit or truncate portions. The builder writes **coverage** and **TODO** style warnings into `processed/mahabharata_validation_report.json` and `shared/knowledge/indexes/build_report.json` rather than claiming completeness.

## Processed outputs

| File | Description |
|------|-------------|
| `processed/mahabharata_chunks.jsonl` | Paragraph-level chunks with parva/section citation. |
| `processed/mahabharata_parvas.json` | Per-parva metadata and section ranges. |
| `processed/mahabharata_validation_report.json` | Parser/detective summary. |
