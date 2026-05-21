import { describe, expect, it } from "vitest";
import { buildSarathiPrompt, normalizeSarathiResponse, SARATHI_PERSONA_RULES } from "./index.js";

describe("Sarathi persona", () => {
  it("includes simple-language rules", () => {
    expect(SARATHI_PERSONA_RULES.join(" ")).toContain("Use simple, calm English");
    expect(SARATHI_PERSONA_RULES.join(" ")).toContain("Use short sentences");
  });

  it("requires sacred teaching explanations", () => {
    const prompt = buildSarathiPrompt({
      userName: "Pruthvi",
      latestUserMessage: "I am afraid.",
      recentHistory: []
    });
    expect(prompt).toContain("Explain the verse naturally in simple English immediately");
    expect(prompt).toContain("Do not use labels like \"It means:\" or \"For you right now:\"");
    expect(prompt).not.toContain("It means: <");
    expect(prompt).not.toContain("For you right now: <");
  });

  it("requires choosing from the saved greeting set", () => {
    const prompt = buildSarathiPrompt({
      userName: "Pruthvi",
      latestUserMessage: "I am tired.",
      recentHistory: []
    });

    expect(prompt).toContain("Start with exactly one greeting from this saved set");
    expect(prompt).toContain("My dear Pruthvi");
    expect(prompt).toContain("Do not always use My dear friend");
  });

  it("includes approved fallback Gita verses when no retrieved source is present", () => {
    const prompt = buildSarathiPrompt({
      userName: "Pruthvi",
      latestUserMessage: "Give me Gita guidance.",
      recentHistory: []
    });

    expect(prompt).toContain("Approved fallback Gita verses");
    expect(prompt).toContain("Bhagavad Gita 2.47");
    expect(prompt).toContain("Always include exactly one grounded verse reference");
  });

  it("normalizes responses into one source line without duplicate references", () => {
    const response = normalizeSarathiResponse({
      responseText: "O brave heart, I hear your pain.\n\nIn Bhagavad Gita 2.48, it says to do your duty with balance. This means act calmly.\n\n*Do your duty with balance, and do not let success or failure shake your inner steadiness.* -Bhagavad Gita 2.48",
      userMessage: "How to choose between mother and wife?",
      ragPassages: []
    });

    expect(response.match(/Bhagavad Gita 2\.48/g)?.length).toBe(1);
    expect(response).not.toContain("In Bhagavad Gita 2.48");
    expect(response).toContain("*Do your duty with balance");
    expect(response.trim().endsWith("-Bhagavad Gita 2.48")).toBe(true);
  });

  it("prefers retrieved RAG source over an unrelated model final reference", () => {
    const response = normalizeSarathiResponse({
      responseText: "My dear friend, I see your effort.\n\nFocus on sincere action.\n\n*Pleasure and pain come and go like seasons.* -Bhagavad Gita 2.14",
      userMessage: "I worked hard but failed.",
      ragPassages: [{
        id: "gita_02_047_besant",
        collection: "gita",
        work: "Bhagavad Gita",
        title: "Bhagavad Gita 2.47",
        citation: "Bhagavad Gita 2.47",
        text: "Thy right is to work only, but never to its fruits.",
        themes: ["karma_yoga"]
      }]
    });

    expect(response).toContain("Bhagavad Gita 2.47");
    expect(response).not.toContain("Bhagavad Gita 2.14");
  });

  it("rejects noisy narrative Mahabharata chunks for practical failure prompts", () => {
    const response = normalizeSarathiResponse({
      responseText: "My dear Pruthvi, I hear your disappointment.\n\nKeep your effort steady.",
      userMessage: "I worked hard but failed.",
      ragPassages: [{
        id: "mahabharata_03_indralokagamana_para_001",
        collection: "mahabharata",
        work: "Mahabharata",
        title: "Mahabharata, Vana Parva",
        citation: "Mahabharata, Vana Parva, Section XLV",
        text: "(Indralokagamana Parva)\n\nVaisampayana said, \"After the Lokapalas had gone away, Arjuna--that slayer of all foes--began to think, O monarch, of the car of Indra! And as Gudakesa gifted with great intelligence was thinking of it, the car endued with effulgence came there.\"",
        themes: ["action", "duty", "karma yoga"]
      }]
    });

    expect(response).toContain("Bhagavad Gita 2.47");
    expect(response).not.toContain("Indralokagamana");
  });

  it("injects retrieved RAG passages into the prompt", () => {
    const prompt = buildSarathiPrompt({
      userName: "Pruthvi",
      latestUserMessage: "I failed at work.",
      recentHistory: [],
      ragPassages: [
        {
          id: "gita_02_047_besant",
          collection: "gita",
          work: "Bhagavad Gita",
          title: "Bhagavad Gita 2.47",
          citation: "Bhagavad Gita 2.47",
          text: "Thy right is to work only, but never to its fruits.",
          themes: ["karma_yoga"],
          score: 8
        }
      ]
    });

    expect(prompt).toContain("Retrieved scripture context:");
    expect(prompt).toContain("[1] Bhagavad Gita 2.47");
    expect(prompt).toContain("Thy right is to work only");
  });

  it("accepts RAG passages without optional metadata at the API boundary", () => {
    const prompt = buildSarathiPrompt({
      userName: "Pruthvi",
      latestUserMessage: "I worked hard but failed.",
      recentHistory: [],
      ragPassages: [
        {
          id: "gita_02_047_smoke",
          collection: "gita",
          source: "Bhagavad Gita",
          citation: "Bhagavad Gita 2.47",
          text: "You have a right to perform your prescribed duties, but you are not entitled to the fruits of your actions."
        } as never
      ]
    });

    expect(prompt).toContain("[1] Bhagavad Gita 2.47 - Bhagavad Gita");
    expect(prompt).toContain("You have a right to perform your prescribed duties");
  });
});
