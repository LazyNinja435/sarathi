import { GoogleGenerativeAI } from "@google/generative-ai";

export interface GenerateSarathiResponseInput {
  apiKey: string;
  model: string;
  prompt: string;
  provider?: "gemini" | "deepseek" | "openrouter";
}

export async function generateSarathiResponse(input: GenerateSarathiResponseInput) {
  if (input.provider === "openrouter" || input.provider === "deepseek") {
    const isDeepSeek = input.provider === "deepseek";
    const response = await fetch(isDeepSeek ? "https://api.deepseek.com/chat/completions" : "https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${input.apiKey}`,
        "Content-Type": "application/json",
        ...(isDeepSeek ? {} : {
          "HTTP-Referer": "https://talkto.sreekrishna.uk",
          "X-Title": "Sarathi"
        })
      },
      body: JSON.stringify({
        model: input.model,
        messages: [{ role: "user", content: input.prompt }]
      })
    });

    if (!response.ok) {
      throw new Error(`${isDeepSeek ? "DeepSeek" : "OpenRouter"} request failed with ${response.status}`);
    }

    const data = await response.json() as {
      choices?: Array<{ message?: { content?: string } }>;
      usage?: unknown;
    };
    const text = data.choices?.[0]?.message?.content?.trim();
    if (!text) {
      throw new Error(`${isDeepSeek ? "DeepSeek" : "OpenRouter"} returned an empty response.`);
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
