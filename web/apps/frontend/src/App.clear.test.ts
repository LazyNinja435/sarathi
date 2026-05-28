import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

describe("chat clear behavior", () => {
  it("keeps the Clear button app-side only", () => {
    const source = readFileSync(fileURLToPath(new URL("./App.tsx", import.meta.url)), "utf8");

    expect(source).not.toContain("clearConversationHistory");
    expect(source).toContain("setMessages([])");
  });
});

describe("server-managed chat UI", () => {
  it("does not expose provider API key controls", () => {
    const source = readFileSync(fileURLToPath(new URL("./App.tsx", import.meta.url)), "utf8");

    expect(source).not.toContain("Use My Key");
    expect(source).not.toContain("Get Free Key");
    expect(source).not.toContain("API key");
    expect(source).not.toContain("readGeminiApiKey");
    expect(source).not.toContain("saveApiMode");
  });

  it("shows dismissible sign-in personalization copy", () => {
    const source = readFileSync(fileURLToPath(new URL("./App.tsx", import.meta.url)), "utf8");

    expect(source).toContain("Sign in to make Sarathi remember what matters to you.");
    expect(source).toContain("Dismiss sign-in reminder");
  });
});
