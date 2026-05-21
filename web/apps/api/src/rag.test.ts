import { describe, expect, it } from "vitest";
import { searchBackendRagPassages } from "./rag.js";

describe("backend RAG", () => {
  it("loads the canonical enriched Gita file and retrieves a relevant verse", () => {
    const passages = searchBackendRagPassages("I worked hard but failed and feel anxious about the result.", 5);

    expect(passages.length).toBeGreaterThan(0);
    expect(passages.some((passage) => passage.citation === "Bhagavad Gita 2.47")).toBe(true);
    expect(passages[0].collection).toBe("gita");
    expect(passages[0].text.trim().length).toBeGreaterThan(0);
  });
});
