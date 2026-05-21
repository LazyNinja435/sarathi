import type { ChatMessage, RagPassage, ShortTermMemory, UserMemory } from "@sarathi/shared-types";
import {
  SARATHI_PERSONA,
  SARATHI_FALLBACK_GITA_VERSES,
  SARATHI_PERSONA_RULES,
  SARATHI_PLAIN_TEXT_REPLY_INSTRUCTION,
  SARATHI_RESPONSE_SHAPE_RULES
} from "./generatedPromptContract.js";

export { SARATHI_FALLBACK_GITA_VERSES, SARATHI_PERSONA, SARATHI_PERSONA_RULES, SARATHI_RESPONSE_SHAPE_RULES };

export interface BuildSarathiPromptInput {
  userName?: string;
  latestUserMessage: string;
  recentHistory: ChatMessage[];
  shortTermMemory?: ShortTermMemory;
  longTermMemory?: UserMemory | null;
  ragPassages?: RagPassage[];
}

const finalReferencePattern = /-(Bhagavad Gita|Mahabharata)[^\n]+$/i;
const gitaReferencePattern = /Bhagavad Gita\s+\d+\.\d+(?:-\d+)?/i;

function fallbackVerseFor(text: string) {
  const lower = text.toLowerCase();
  if (/\b(worry|worried|anxious|anxiety|mind|restless|thought)\b/.test(lower)) {
    return SARATHI_FALLBACK_GITA_VERSES.find((verse) => verse.startsWith("Bhagavad Gita 6.26")) ?? SARATHI_FALLBACK_GITA_VERSES[0];
  }
  if (/\b(result|failed|failure|success|work|effort|control)\b/.test(lower)) {
    return SARATHI_FALLBACK_GITA_VERSES.find((verse) => verse.startsWith("Bhagavad Gita 2.47")) ?? SARATHI_FALLBACK_GITA_VERSES[0];
  }
  if (/\b(tired|pain|sad|hurt|hard|suffer|season)\b/.test(lower)) {
    return SARATHI_FALLBACK_GITA_VERSES.find((verse) => verse.startsWith("Bhagavad Gita 2.14")) ?? SARATHI_FALLBACK_GITA_VERSES[0];
  }
  if (/\b(lost|burden|surrender|trust|afraid|fear)\b/.test(lower)) {
    return SARATHI_FALLBACK_GITA_VERSES.find((verse) => verse.startsWith("Bhagavad Gita 18.66")) ?? SARATHI_FALLBACK_GITA_VERSES[0];
  }
  return SARATHI_FALLBACK_GITA_VERSES.find((verse) => verse.startsWith("Bhagavad Gita 2.47")) ?? SARATHI_FALLBACK_GITA_VERSES[0];
}

function looksLikeUsableQuote(passage: RagPassage) {
  const text = (passage.translation || passage.text).trim();
  if (!text || text.length > 420) return false;
  if (/^\([^)]+parva\)/i.test(text)) return false;
  if (/\b(Vaisampayana said|Sanjaya said|Dhritarashtra said|O monarch)\b/i.test(text) && passage.collection !== "gita") return false;
  return true;
}

function selectedRagPassage(userMessage: string, ragPassages: RagPassage[]) {
  const lower = userMessage.toLowerCase();
  const wantsPracticalGita = /\b(work|worked|failed|failure|result|effort|anxious|worry|worried|afraid|future|tired|sad|duty|dharma)\b/.test(lower);
  const usable = ragPassages.filter(looksLikeUsableQuote);
  if (wantsPracticalGita) {
    return usable.find((passage) => passage.collection === "gita") ?? null;
  }
  return usable[0] ?? null;
}

export function ensureGroundedGitaVerse(responseText: string, userMessage: string): string {
  if (gitaReferencePattern.test(responseText)) return responseText;
  const fallback = fallbackVerseFor(`${userMessage}\n${responseText}`);
  const [reference, teaching] = fallback.split(": ");
  return `${responseText.trim()}\n\n*${teaching}* -${reference}`;
}

function selectedSourceLine(responseText: string, userMessage: string, ragPassages: RagPassage[] = []) {
  const primaryRag = selectedRagPassage(userMessage, ragPassages);
  if (primaryRag) {
    return `*${(primaryRag.translation || primaryRag.text).trim().slice(0, 220)}* -${primaryRag.citation}`;
  }

  const explicitFinal = responseText.trim().split("\n").reverse().find((line) => finalReferencePattern.test(line.trim()));
  if (explicitFinal) return explicitFinal.trim().replace(/^"(.+)"\s*-/, "*$1* -");

  const citedReference = responseText.match(gitaReferencePattern)?.[0];
  const citedPassage = citedReference
    ? ragPassages.find((passage) => passage.citation.toLowerCase() === citedReference.toLowerCase())
    : undefined;
  if (citedPassage) {
    return `*${(citedPassage.translation || citedPassage.text).trim().slice(0, 220)}* -${citedPassage.citation}`;
  }

  const fallback = fallbackVerseFor(`${userMessage}\n${responseText}`);
  const [reference, teaching] = fallback.split(": ");
  return `*${teaching}* -${reference}`;
}

function removeReferenceDuplication(text: string) {
  return text
    .split("\n")
    .filter((line) => !finalReferencePattern.test(line.trim()))
    .join("\n")
    .replace(/\b[Tt]he retrieved teaching\s*\(\)\s*/g, "This teaching ")
    .replace(/\b(?:In|From|As)\s+(?:the\s+)?Bhagavad Gita\s+\d+\.\d+(?:-\d+)?,?\s*(?:it says|the teaching is|Krishna teaches|we learn)?\s*/gi, "")
    .replace(/\bBhagavad Gita\s+\d+\.\d+(?:-\d+)?\b[:,]?\s*/gi, "")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function splitParagraphs(text: string) {
  return text.split(/\n{2,}/).map((part) => part.trim()).filter(Boolean);
}

export function normalizeSarathiResponse(input: {
  responseText: string;
  userMessage: string;
  ragPassages?: RagPassage[];
}): string {
  const sourceLine = selectedSourceLine(input.responseText, input.userMessage, input.ragPassages);
  const body = removeReferenceDuplication(input.responseText);
  const paragraphs = splitParagraphs(body);
  if (paragraphs.length > 1 && /^((my dear|o)\b[^.!?]*,?)$/i.test(paragraphs[0])) {
    paragraphs[1] = `${paragraphs[0]} ${paragraphs[1]}`;
    paragraphs.shift();
  }
  const first = paragraphs[0] || "My dear friend, I hear what rests on your heart.";
  const second = paragraphs.slice(1).join(" ") || "Take one small truthful step with steadiness. Let the verse guide your action, not your fear.";
  return [first, second, sourceLine].join("\n\n");
}

export function buildSarathiPrompt(input: BuildSarathiPromptInput): string {
  const userName = input.userName?.trim() || "friend";
  const recentHistory = input.recentHistory
    .slice(-8)
    .map((message) => `${message.role}: ${message.text}`)
    .join("\n");
  const savedNotes = input.longTermMemory?.savedUserNotes?.length
    ? input.longTermMemory.savedUserNotes.map((note) => `- ${note}`).join("\n")
    : "None.";
  const ragContext = input.ragPassages?.length
    ? [
        "Retrieved scripture context:",
        ...input.ragPassages.slice(0, 5).flatMap((passage, index) => [
          `[${index + 1}] ${passage.citation} - ${passage.work ?? (passage as { source?: string }).source ?? "Scripture"}${passage.title ? `, ${passage.title}` : ""}`,
          (passage.translation || passage.text).slice(0, 900),
          passage.themes?.length ? `Themes: ${passage.themes.join(", ")}` : "",
          ""
        ]),
        "Use these retrieved passages before the fallback verse list.",
        "If a retrieved Bhagavad Gita verse is relevant, cite that verse in the final grounding line.",
        ""
      ].join("\n")
    : "";

  return [
    SARATHI_PERSONA.replaceAll("<userName>", userName),
    "",
    "Privacy and memory rules:",
    "Use long-term memory only as gentle context.",
    "Do not invent private facts about the user.",
    "Do not say you saved something unless the user clearly asked to remember it.",
    "",
    "Long-term saved notes:",
    savedNotes,
    "",
    "Short-term memory:",
    JSON.stringify(input.shortTermMemory ?? {}, null, 2),
    "",
    ragContext,
    "Recent conversation:",
    recentHistory || "No recent messages.",
    "",
    "User message:",
    input.latestUserMessage,
    "",
    SARATHI_PLAIN_TEXT_REPLY_INSTRUCTION
  ].join("\n");
}
