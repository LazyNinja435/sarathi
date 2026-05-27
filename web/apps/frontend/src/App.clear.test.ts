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
