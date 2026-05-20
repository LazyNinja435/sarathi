import { describe, expect, it, vi } from "vitest";
import { ragDebugFor, searchRagPassages } from "./rag.js";

describe("web RAG search", () => {
  it("returns relevant passages and debug sources", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => ({
      ok: true,
      json: async () => ({
        version: 1,
        documents: [
          {
            id: "gita_02_047_besant",
            collection: "gita",
            work: "Bhagavad Gita",
            title: "Bhagavad Gita 2.47",
            citation: "Bhagavad Gita 2.47",
            text: "Thy right is to work only, but never to its fruits.",
            translation: "Thy right is to work only, but never to its fruits.",
            themes: ["karma_yoga"]
          },
          {
            id: "gita_06_026_besant",
            collection: "gita",
            work: "Bhagavad Gita",
            title: "Bhagavad Gita 6.26",
            citation: "Bhagavad Gita 6.26",
            text: "Bring the mind back under the control of the Self.",
            translation: "Bring the mind back under the control of the Self.",
            themes: ["mind"]
          }
        ],
        concepts: [{
          id: "concept_karma_yoga",
          name: "Karma Yoga",
          aliases: ["duty without attachment"],
          description: "",
          relatedIds: ["gita_02_047"],
          searchTerms: ["action", "duty", "fruits of action"]
        }]
      })
    })));

    const results = await searchRagPassages("I worked hard but failed. Help me with duty.", 3);

    expect(results[0].citation).toBe("Bhagavad Gita 2.47");
    expect(ragDebugFor(results)).toMatchObject({
      used: true,
      sources: expect.arrayContaining(["Bhagavad Gita 2.47"])
    });
  });

  it("prefers Gita verses over broad Mahabharata narrative chunks for failure prompts", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => ({
      ok: true,
      json: async () => ({
        version: 1,
        documents: [
          {
            id: "mahabharata_03_indralokagamana_para_001",
            collection: "mahabharata",
            work: "Mahabharata",
            title: "Mahabharata, Vana Parva",
            citation: "Mahabharata, Vana Parva, Section XLV",
            text: "(Indralokagamana Parva) Vaisampayana said that Arjuna worked hard and thought of Indra.",
            translation: "",
            themes: ["action", "duty", "karma yoga"]
          },
          {
            id: "gita_02_047_besant",
            collection: "gita",
            work: "Bhagavad Gita",
            title: "Bhagavad Gita 2.47",
            citation: "Bhagavad Gita 2.47",
            text: "Thy right is to work only, but never to its fruits.",
            translation: "Thy right is to work only, but never to its fruits.",
            themes: ["karma_yoga"]
          }
        ],
        concepts: [{
          id: "concept_karma_yoga",
          name: "Karma Yoga",
          aliases: ["duty without attachment"],
          description: "",
          relatedIds: ["gita_02_047"],
          searchTerms: ["action", "duty", "fruits of action", "failed"]
        }]
      })
    })));

    const results = await searchRagPassages("I worked hard but failed.", 2);

    expect(results[0].citation).toBe("Bhagavad Gita 2.47");
  });
});
