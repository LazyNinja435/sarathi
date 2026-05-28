import { describe, expect, it } from "vitest";
import { createDefaultUserMemory, SUPPORTED_AI_PROVIDERS } from "./index.js";

describe("memory defaults", () => {
  it("uses simple devotional defaults", () => {
    const memory = createDefaultUserMemory("2026-05-19T00:00:00.000Z");
    expect(memory.preferredLanguageLevel).toBe("simple");
    expect(memory.preferredResponseStyle).toBe("quote_simple_meaning_practical_guidance");
    expect(memory.spiritualTonePreference).toBe("krishna_inspired_practical");
    expect(memory.savedUserNotes).toEqual([]);
  });
});

describe("AI providers", () => {
  it("lists the server-managed cascade providers", () => {
    expect(SUPPORTED_AI_PROVIDERS).toEqual(["gemini", "deepseek", "openrouter"]);
  });
});
