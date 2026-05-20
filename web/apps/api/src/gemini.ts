import { GoogleGenerativeAI } from "@google/generative-ai";

export interface GenerateSarathiResponseInput {
  apiKey: string;
  model: string;
  prompt: string;
  provider?: "gemini" | "openrouter";
}

export async function generateSarathiResponse(input: GenerateSarathiResponseInput) {
  if (input.provider === "openrouter") {
    const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${input.apiKey}`,
        "Content-Type": "application/json",
        "HTTP-Referer": "https://talkto.sreekrishna.uk",
        "X-Title": "Sarathi"
      },
      body: JSON.stringify({
        model: input.model,
        messages: [{ role: "user", content: input.prompt }]
      })
    });

    if (!response.ok) {
      throw new Error(`OpenRouter request failed with ${response.status}`);
    }

    const data = await response.json() as {
      choices?: Array<{ message?: { content?: string } }>;
      usage?: unknown;
    };
    const text = data.choices?.[0]?.message?.content?.trim();
    if (!text) {
      throw new Error("OpenRouter returned an empty response.");
    }
    return { text, usage: data.usage };
  }

  const genAi = new GoogleGenerativeAI(input.apiKey);
  const model = genAi.getGenerativeModel({ model: input.model });
  const result = await model.generateContent(input.prompt);
  const text = result.response.text();

  return {
    text,
    usage: result.response.usageMetadata
  };
}
