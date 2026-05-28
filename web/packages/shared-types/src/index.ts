export type ChatRole = "user" | "assistant" | "system";
export const SUPPORTED_AI_PROVIDERS = ["gemini", "deepseek", "openrouter"] as const;
export type AiProviderId = typeof SUPPORTED_AI_PROVIDERS[number];

export interface ChatMessage {
  id: string;
  role: ChatRole;
  text: string;
  createdAt: string;
  source?: "frontend" | "api" | "firestore";
  memoryUsed?: boolean;
  provider?: string;
}

export interface Conversation {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  lastMessagePreview: string;
  modelProvider: AiProviderId;
  messageCount: number;
  archived: boolean;
}

export interface ShortTermMemory {
  currentEmotion?: string;
  currentConcern?: string;
  importantContext?: string;
  preferredGuidanceStyle?: string;
  lastGuidanceTheme?: string;
}

export interface UserMemory {
  preferredLanguageLevel: "simple";
  preferredResponseStyle: "quote_simple_meaning_practical_guidance";
  spiritualTonePreference: "krishna_inspired_practical";
  savedUserNotes: string[];
  updatedAt: string;
}

export interface UserPreferences {
  displayName?: string;
  preferredLanguageLevel: "simple";
  preferredResponseStyle: "quote_simple_meaning_practical_guidance";
  spiritualTonePreference: "krishna_inspired_practical";
  updatedAt: string;
}

export interface RagPassage {
  id: string;
  collection: "gita" | "mahabharata" | string;
  work: string;
  title: string;
  citation: string;
  text: string;
  translation?: string;
  themes: string[];
  sourceTitle?: string;
  sourceUrl?: string;
  score?: number;
}

export function createDefaultUserMemory(now = new Date().toISOString()): UserMemory {
  return {
    preferredLanguageLevel: "simple",
    preferredResponseStyle: "quote_simple_meaning_practical_guidance",
    spiritualTonePreference: "krishna_inspired_practical",
    savedUserNotes: [],
    updatedAt: now
  };
}

export function createDefaultUserPreferences(now = new Date().toISOString()): UserPreferences {
  return {
    preferredLanguageLevel: "simple",
    preferredResponseStyle: "quote_simple_meaning_practical_guidance",
    spiritualTonePreference: "krishna_inspired_practical",
    updatedAt: now
  };
}
