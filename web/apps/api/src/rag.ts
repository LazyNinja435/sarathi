import type { RagPassage } from "@sarathi/shared-types";
import { existsSync, readFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

interface EnrichedGitaRecord {
  id: string;
  source: {
    text: string;
    chapter: number;
    verse: number;
    citation: string;
    chapter_title?: string;
    source_title?: string;
    source_url?: string;
  };
  content: {
    translation: string;
  };
  retrieval: {
    primary_topics: string[];
    secondary_topics: string[];
    user_prompt_keywords: string[];
    emotional_states: string[];
    life_situations: string[];
    spiritual_concepts: string[];
    intent_matches: string[];
  };
  teaching: {
    one_line_summary: string;
    practical_guidance: string;
    core_message: string;
  };
  rag: {
    search_text: string;
    embedding_text: string;
    priority_score: number;
    confidence: number;
  };
}

const stopWords = new Set([
  "a", "about", "and", "are", "as", "at", "be", "but", "for", "from", "give", "how", "i", "in", "is", "it",
  "me", "my", "of", "on", "or", "the", "this", "to", "what", "when", "with", "you"
]);

let cachedRecords: EnrichedGitaRecord[] | null = null;

function normalize(value: string) {
  return value.toLowerCase().normalize("NFKD").replace(/[^\w\s.-]/g, " ");
}

function tokenize(value: string) {
  return normalize(value)
    .split(/\s+/)
    .map((token) => token.trim())
    .filter((token) => token.length > 2 && !stopWords.has(token));
}

function repoRootCandidates() {
  const here = dirname(fileURLToPath(import.meta.url));
  return [
    process.env.SARATHI_KNOWLEDGE_ROOT,
    process.cwd(),
    resolve(process.cwd(), ".."),
    resolve(process.cwd(), "../.."),
    resolve(here, "../../.."),
    resolve(here, "../../../.."),
    resolve(here, "../../../../..")
  ].filter(Boolean) as string[];
}

function resolveKnowledgeFile() {
  for (const root of repoRootCandidates()) {
    const candidate = join(root, "knowledge", "sources", "gita", "processed", "gita_verses.jsonl");
    if (existsSync(candidate)) return candidate;
  }
  throw new Error("Canonical Gita knowledge file not found.");
}

function loadRecords() {
  if (cachedRecords) return cachedRecords;
  const filePath = resolveKnowledgeFile();
  cachedRecords = readFileSync(filePath, "utf8")
    .split(/\r?\n/)
    .filter((line) => line.trim().length > 0)
    .map((line) => JSON.parse(line) as EnrichedGitaRecord);
  return cachedRecords;
}

function scoreRecord(record: EnrichedGitaRecord, queryTokens: string[]) {
  const searchable = normalize([
    record.source.citation,
    record.source.chapter_title ?? "",
    record.content.translation,
    record.rag.search_text,
    record.rag.embedding_text,
    record.retrieval.primary_topics.join(" "),
    record.retrieval.intent_matches.join(" ")
  ].join(" "));
  let lexicalScore = 0;
  for (const token of queryTokens) {
    if (searchable.includes(token)) lexicalScore += 1;
    if (normalize(record.retrieval.primary_topics.join(" ")).includes(token)) lexicalScore += 2;
    if (normalize(record.retrieval.user_prompt_keywords.join(" ")).includes(token)) lexicalScore += 3;
    if (normalize(record.source.citation).includes(token)) lexicalScore += 4;
  }
  return lexicalScore + record.rag.priority_score * 4 + record.rag.confidence;
}

function toPassage(record: EnrichedGitaRecord, score: number): RagPassage {
  return {
    id: record.id,
    collection: "gita",
    work: "Bhagavad Gita",
    title: record.source.citation,
    citation: record.source.citation,
    text: record.content.translation,
    translation: record.content.translation,
    themes: [...record.retrieval.primary_topics, ...record.retrieval.intent_matches],
    sourceTitle: record.source.source_title,
    sourceUrl: record.source.source_url,
    score
  };
}

export function searchBackendRagPassages(query: string, limit = 5): RagPassage[] {
  const queryTokens = tokenize(query);
  if (queryTokens.length === 0) return [];
  return loadRecords()
    .map((record) => ({ record, score: scoreRecord(record, queryTokens) }))
    .filter(({ score }) => score > 0)
    .sort((a, b) => b.score - a.score)
    .slice(0, limit)
    .map(({ record, score }) => toPassage(record, score));
}
