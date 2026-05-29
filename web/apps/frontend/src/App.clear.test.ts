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
  function readUiSources() {
    return [
      readFileSync(fileURLToPath(new URL("./App.tsx", import.meta.url)), "utf8"),
      readFileSync(fileURLToPath(new URL("./components/sarathi/SarathiChatPanel.tsx", import.meta.url)), "utf8"),
      readFileSync(fileURLToPath(new URL("./sarathiDesigns.tsx", import.meta.url)), "utf8"),
    ].join("\n");
  }

  it("does not expose provider API key controls", () => {
    const source = readUiSources();

    expect(source).not.toContain("Use My Key");
    expect(source).not.toContain("Get Free Key");
    expect(source).not.toContain("API key");
    expect(source).not.toContain("readGeminiApiKey");
    expect(source).not.toContain("saveApiMode");
  });

  it("shows dismissible sign-in personalization copy", () => {
    const source = readUiSources();

    expect(source).toContain("Sign in to make Sarathi remember what matters to you.");
    expect(source).toContain("Dismiss sign-in reminder");
  });

  it("registers the focused Sarathi mock design route", () => {
    const source = readFileSync(fileURLToPath(new URL("./App.tsx", import.meta.url)), "utf8");

    expect(source).toContain('/mock/sarathi-designs');
    expect(source).toContain('/mock/sarathi-design-2');
    expect(source).toContain('/mock/sarathi-design-2-signed-in');
  });

  it("focuses old mock routes onto Design 2 and hides empty references", () => {
    const source = readUiSources();

    expect(source).toContain("SarathiChatPanel");
    expect(source).toContain('<Navigate to="/mock/sarathi-design-2" replace />');
    expect(source).toContain("Your Sacred Memory");
    expect(source).toContain("Clarify Your Soul");
    expect(source).not.toContain("Continue as guest</a>");
    expect(source).not.toContain("<span>N/A</span>");
  });

  it("keeps production chat presentation outside the mock design module", () => {
    const appSource = readFileSync(fileURLToPath(new URL("./App.tsx", import.meta.url)), "utf8");
    const productionChatSource = readFileSync(fileURLToPath(new URL("./components/sarathi/SarathiChatPanel.tsx", import.meta.url)), "utf8");

    expect(appSource).toContain('from "./components/sarathi/SarathiChatPanel"');
    expect(appSource).toContain('from "./sarathiDesigns"');
    expect(productionChatSource).toContain("type SarathiChatPanelProps");
    expect(productionChatSource).toContain("references.length > 0");
    expect(productionChatSource).toContain("Your Sacred Memory");
  });

  it("aligns user chat bubbles to the right side of the thread", () => {
    const componentSource = readFileSync(fileURLToPath(new URL("./components/sarathi/SarathiChatPanel.tsx", import.meta.url)), "utf8");
    const cssSource = readFileSync(fileURLToPath(new URL("./styles.css", import.meta.url)), "utf8");

    expect(componentSource).toContain('className="krishna-user-row"');
    expect(componentSource).toContain('className="krishna-user-card"');
    expect(cssSource).toMatch(/\.krishna-user-row\s*\{[^}]*justify-content:\s*flex-end/s);
    expect(cssSource).toMatch(/\.krishna-user-card\s*\{[^}]*margin-left:\s*auto/s);
  });

  it("shows Verse of the Day in Sanskrit, not romanized transliteration", () => {
    const source = readUiSources();

    expect(source).toContain("getVerseOfTheDay");
    expect(source).toContain("verseOfTheDay.sanskrit");
    expect(source).not.toContain("karmany evadhikaras te ma phalesu kadacana");
  });

  it("makes the Krishna right-side cards a collapsible drawer on smaller screens", () => {
    const componentSource = readFileSync(fileURLToPath(new URL("./components/sarathi/SarathiChatPanel.tsx", import.meta.url)), "utf8");
    const cssSource = readFileSync(fileURLToPath(new URL("./styles.css", import.meta.url)), "utf8");

    expect(componentSource).toContain("isDesktopSidebarDefaultOpen");
    expect(componentSource).toContain('aria-controls="krishna-sidebar-cards"');
    expect(componentSource).toContain("setSidebarOpen");
    expect(componentSource).toContain("sidebar-open");
    expect(componentSource).toContain("sidebar-collapsed");
    expect(cssSource).toMatch(/\.krishna-sidebar-toggle\s*\{/s);
    expect(cssSource).toMatch(/\.krishna-reference-grid\.sidebar-collapsed\s*\{/s);
    expect(cssSource).toMatch(/@media \(max-width: 980px\)[\s\S]*\.krishna-sidebar\s*\{[\s\S]*position:\s*fixed/s);
    expect(cssSource).toMatch(/@media \(max-width: 980px\)[\s\S]*\.krishna-sidebar\.is-collapsed\s*\{[\s\S]*transform:\s*translateX\(calc\(100% \+ 1\.25rem\)\)/s);
  });
});

describe("local Firebase bootstrap", () => {
  it("has non-secret local fallback values so mock routes can render without env files", () => {
    const source = readFileSync(fileURLToPath(new URL("./firebase.ts", import.meta.url)), "utf8");

    expect(source).toContain("sarathi-local-api-key");
    expect(source).toContain("sarathi-local.firebaseapp.com");
  });
});

describe("Clarify Your Soul mock settings", () => {
  it("defines Sarathi-specific soul memory sections and future schema shape", () => {
    const source = readFileSync(fileURLToPath(new URL("./SoulMemorySettings.tsx", import.meta.url)), "utf8");

    expect(source).toContain("type SoulMemorySectionKey");
    expect(source).toContain('schema: "sarathi.memory.v1"');
    expect(source).toContain("Clarify Your Soul");
    expect(source).toContain("My Journey");
    expect(source).toContain("Questions I Return To");
    expect(source).toContain("Guidance That Helped Me");
    expect(source).toContain("Practices I");
    expect(source).toContain("Personal Reflections");
    expect(source).toContain("Life Context");
  });

  it("does not add generic assistant memory or deity preference controls", () => {
    const source = readFileSync(fileURLToPath(new URL("./SoulMemorySettings.tsx", import.meta.url)), "utf8");

    expect(source).not.toContain("response style");
    expect(source).not.toContain("spiritual preference");
    expect(source).not.toContain("deity");
    expect(source).not.toContain("personality");
  });
});
