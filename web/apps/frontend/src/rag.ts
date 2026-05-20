import type { RagPassage } from "@sarathi/shared-types";

interface RagConcept {
  id: string;
  name: string;
  aliases: string[];
  description: string;
  relatedIds: string[];
  searchTerms: string[];
}

interface RagBundle {
  version: number;
  documents: RagPassage[];
  concepts: RagConcept[];
}

export interface RagSearchDebug {
  used: boolean;
  sources: string[];
}

const stopWords = new Set([
  "a", "about", "and", "are", "as", "at", "be", "but", "for", "from", "give", "how", "i", "in", "is", "it",
  "me", "my", "of", "on", "or", "the", "this", "to", "what", "when", "with", "you"
]);

let bundlePromise: Promise<RagBundle> | null = null;

function normalize(value: string) {
  return value.toLowerCase().normalize("NFKD").replace(/[^\w\s.-]/g, " ");
}

function tokenize(value: string) {
  return normalize(value)
    .split(/\s+/)
    .map((token) => token.trim())
    .filter((token) => token.length > 2 && !stopWords.has(token));
}

async function loadRagBundle() {
  if (!bundlePromise) {
    bundlePromise = fetch("/rag/sarathi_rag.json")
      .then((response) => {
        if (!response.ok) throw new Error("RAG bundle unavailable");
        return response.json() as Promise<RagBundle>;
      });
  }
  return bundlePromise;
}

function relatedIdMatches(documentId: string, relatedId: string) {
  return documentId === relatedId || documentId.startsWith(`${relatedId}_`);
}

function scoreDocument(document: RagPassage, queryTokens: string[], conceptBoosts: string[]) {
  const themes = document.themes.join(" ");
  const haystack = normalize(`${document.title} ${document.citation} ${themes} ${document.translation || document.text}`);
  let score = 0;
  for (const token of queryTokens) {
    if (haystack.includes(token)) score += 1;
    if (normalize(themes).includes(token)) score += 2;
    if (normalize(document.title).includes(token) || normalize(document.citation).includes(token)) score += 3;
  }
  for (const boost of conceptBoosts) {
    if (relatedIdMatches(document.id, boost)) score += 8;
  }
  if (document.collection === "gita") score += 4;
  if (document.collection === "mahabharata" && /^\([^)]+parva\)/i.test((document.translation || document.text).trim())) score -= 6;
  return score;
}

function conceptBoostsFor(query: string, concepts: RagConcept[]) {
  const normalizedQuery = normalize(query);
  const boosts = new Set<string>();
  for (const concept of concepts) {
    const terms = [concept.name, ...concept.aliases, ...concept.searchTerms].map(normalize);
    if (terms.some((term) => term.length > 2 && normalizedQuery.includes(term))) {
      concept.relatedIds.forEach((id) => boosts.add(id));
    }
  }
  return [...boosts];
}

export async function searchRagPassages(query: string, limit = 5): Promise<RagPassage[]> {
  const bundle = await loadRagBundle();
  const queryTokens = tokenize(query);
  const conceptBoosts = conceptBoostsFor(query, bundle.concepts);
  const scored = bundle.documents
    .map((document) => ({ ...document, score: scoreDocument(document, queryTokens, conceptBoosts) }))
    .filter((document) => (document.score ?? 0) > 0)
    .sort((a, b) => (b.score ?? 0) - (a.score ?? 0));

  return scored.slice(0, limit);
}

export function ragDebugFor(passages: RagPassage[]): RagSearchDebug {
  return {
    used: passages.length > 0,
    sources: passages.map((passage) => passage.citation)
  };
}
