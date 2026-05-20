const GEMINI_KEY_STORAGE = "sarathi.geminiApiKey";
const OPENROUTER_KEY_STORAGE = "sarathi.openRouterApiKey";
const PROVIDER_STORAGE = "sarathi.provider";
const API_MODE_STORAGE = "sarathi.apiMode";
const DEMO_CLIENT_STORAGE = "sarathi.demoClientId";
const GUEST_SESSION_STORAGE = "sarathi.guestSession";

export type ApiMode = "demo" | "user_key";
export type AiProvider = "gemini" | "openrouter";

export function readGeminiApiKey() {
  return localStorage.getItem(GEMINI_KEY_STORAGE) ?? "";
}

export function saveGeminiApiKey(apiKey: string) {
  localStorage.setItem(GEMINI_KEY_STORAGE, apiKey);
}

export function clearGeminiApiKey() {
  localStorage.removeItem(GEMINI_KEY_STORAGE);
}

export function readOpenRouterApiKey() {
  return localStorage.getItem(OPENROUTER_KEY_STORAGE) ?? "";
}

export function saveOpenRouterApiKey(apiKey: string) {
  localStorage.setItem(OPENROUTER_KEY_STORAGE, apiKey);
}

export function clearOpenRouterApiKey() {
  localStorage.removeItem(OPENROUTER_KEY_STORAGE);
}

export function readProvider(): AiProvider {
  return localStorage.getItem(PROVIDER_STORAGE) === "openrouter" ? "openrouter" : "gemini";
}

export function saveProvider(provider: AiProvider) {
  localStorage.setItem(PROVIDER_STORAGE, provider);
}

export function readApiMode(): ApiMode {
  return localStorage.getItem(API_MODE_STORAGE) === "user_key" ? "user_key" : "demo";
}

export function saveApiMode(mode: ApiMode) {
  localStorage.setItem(API_MODE_STORAGE, mode);
}

export function getDemoClientId() {
  const existing = localStorage.getItem(DEMO_CLIENT_STORAGE);
  if (existing) return existing;
  const next = crypto.randomUUID();
  localStorage.setItem(DEMO_CLIENT_STORAGE, next);
  return next;
}

export function readGuestSession() {
  return localStorage.getItem(GUEST_SESSION_STORAGE) === "true";
}

export function saveGuestSession(enabled: boolean) {
  if (enabled) localStorage.setItem(GUEST_SESSION_STORAGE, "true");
  else localStorage.removeItem(GUEST_SESSION_STORAGE);
}
