import { describe, expect, it } from "vitest";
import { createApp } from "./app.js";
import type { ApiEnv } from "./env.js";

const env: ApiEnv = {
  port: 3001,
  allowedOrigin: "https://talkto.sreekrishna.uk",
  geminiModel: "gemini-flash-lite-latest",
  openRouterDemoApiKey: "demo-server-key-that-is-long",
  openRouterDemoModel: "openrouter/free",
  openRouterUserModel: "openrouter/free",
  demoMessageLimit: 10,
  adminEmails: [],
  adminUids: [],
  skipFirebaseAuth: true
};

describe("api", () => {
  it("serves health", async () => {
    const app = createApp({
      env,
      logger: false,
      authVerifier: { verifyIdToken: async () => ({ uid: "u1" }) }
    });
    const response = await app.inject({ method: "GET", url: "/api/health" });
    expect(response.statusCode).toBe(200);
    expect(response.json()).toMatchObject({ ok: true, service: "sarathi-api" });
  });

  it("allows unauthenticated demo chat through the server key", async () => {
    const app = createApp({
      env,
      logger: false,
      authVerifier: { verifyIdToken: async () => ({ uid: "u1" }) },
      generateResponse: async ({ apiKey, model }) => {
        expect(apiKey).toBe(env.openRouterDemoApiKey);
        expect(model).toBe("openrouter/free");
        return { text: "A calm demo answer.", usage: undefined };
      }
    });
    const response = await app.inject({
      method: "POST",
      url: "/api/chat",
      payload: { mode: "demo", conversationId: "c1", latestUserMessage: "Hello" }
    });
    expect(response.statusCode).toBe(200);
    expect(response.json()).toMatchObject({
      provider: "openrouter",
      model: "openrouter/free",
      demo: { messagesRemaining: 9, messageLimit: 10 }
    });
    expect(response.json().assistantMessage).toContain("A calm demo answer.");
    expect(response.json().assistantMessage).toContain("Bhagavad Gita");
  });

  it("requires auth for user provided keys", async () => {
    const app = createApp({
      env,
      logger: false,
      authVerifier: { verifyIdToken: async () => ({ uid: "u1" }) }
    });
    const response = await app.inject({
      method: "POST",
      url: "/api/chat",
      payload: {
        mode: "user_key",
        provider: "gemini",
        apiKey: "secret-key-that-is-long",
        conversationId: "c1",
        latestUserMessage: "Hello"
      }
    });
    expect(response.statusCode).toBe(401);
  });

  it("uses the OpenRouter model for signed-in OpenRouter keys", async () => {
    const app = createApp({
      env,
      logger: false,
      authVerifier: { verifyIdToken: async () => ({ uid: "u1", name: "Pruthvi" }) },
      generateResponse: async ({ apiKey, model, provider }) => {
        expect(apiKey).toBe("openrouter-user-key");
        expect(provider).toBe("openrouter");
        expect(model).toBe("openrouter/free");
        return { text: "A calm OpenRouter answer.", usage: undefined };
      }
    });
    const response = await app.inject({
      method: "POST",
      url: "/api/chat",
      headers: { authorization: "Bearer test-token" },
      payload: {
        mode: "user_key",
        provider: "openrouter",
        apiKey: "openrouter-user-key",
        conversationId: "c1",
        latestUserMessage: "Hello"
      }
    });
    expect(response.statusCode).toBe(200);
    expect(response.json()).toMatchObject({ provider: "openrouter", model: "openrouter/free" });
  });

  it("does not expose the api key in prompts or responses", async () => {
    const secret = "secret-key-that-is-long";
    let promptSeenByProvider = "";
    const app = createApp({
      env,
      logger: false,
      authVerifier: { verifyIdToken: async () => ({ uid: "u1" }) },
      generateResponse: async ({ prompt }) => {
        promptSeenByProvider = prompt;
        return { text: "A calm answer.", usage: undefined };
      }
    });
    const response = await app.inject({
      method: "POST",
      url: "/api/chat",
      headers: { authorization: "Bearer test-token" },
      payload: {
        mode: "user_key",
        provider: "gemini",
        apiKey: secret,
        conversationId: "c1",
        latestUserMessage: "Hello",
        recentHistory: []
      }
    });
    expect(response.statusCode).toBe(200);
    expect(response.body).not.toContain(secret);
    expect(promptSeenByProvider).not.toContain(secret);
  });

  it("adds a grounded Gita verse when the model omits one", async () => {
    const app = createApp({
      env,
      logger: false,
      authVerifier: { verifyIdToken: async () => ({ uid: "u1" }) },
      generateResponse: async () => ({ text: "My dear friend, take one steady breath.", usage: undefined })
    });
    const response = await app.inject({
      method: "POST",
      url: "/api/chat",
      payload: { mode: "demo", conversationId: "c1", latestUserMessage: "I failed at work." }
    });
    expect(response.statusCode).toBe(200);
    expect(response.json().assistantMessage).toContain("Bhagavad Gita 2.47");
  });

  it("normalizes duplicate demo references into one final source line", async () => {
    const app = createApp({
      env,
      logger: false,
      authVerifier: { verifyIdToken: async () => ({ uid: "u1" }) },
      generateResponse: async () => ({
        text: "O brave heart, I hear your pain.\n\nIn the Bhagavad Gita 2.48, it says to do your duty with balance. This means act calmly.\n\n*Do your duty with balance, and do not let success or failure shake your inner steadiness.* -Bhagavad Gita 2.48",
        usage: undefined
      })
    });
    const response = await app.inject({
      method: "POST",
      url: "/api/chat",
      payload: { mode: "demo", conversationId: "c1", latestUserMessage: "How to choose between mother and wife?" }
    });
    const text = response.json().assistantMessage as string;
    expect(text.match(/Bhagavad Gita 2\.48/g)?.length).toBe(1);
    expect(text).not.toContain("In the Bhagavad Gita 2.48");
    expect(text.trim().endsWith("-Bhagavad Gita 2.48")).toBe(true);
  });

  it("passes RAG passages into the model prompt", async () => {
    let promptSeenByProvider = "";
    const app = createApp({
      env,
      logger: false,
      authVerifier: { verifyIdToken: async () => ({ uid: "u1" }) },
      generateResponse: async ({ prompt }) => {
        promptSeenByProvider = prompt;
        return { text: "A grounded answer. Bhagavad Gita 2.47", usage: undefined };
      }
    });
    const response = await app.inject({
      method: "POST",
      url: "/api/chat",
      payload: {
        mode: "demo",
        conversationId: "c1",
        latestUserMessage: "I failed.",
        ragPassages: [{
          id: "gita_02_047_besant",
          collection: "gita",
          work: "Bhagavad Gita",
          title: "Bhagavad Gita 2.47",
          citation: "Bhagavad Gita 2.47",
          text: "Thy right is to work only, but never to its fruits.",
          themes: ["karma_yoga"]
        }]
      }
    });
    expect(response.statusCode).toBe(200);
    expect(promptSeenByProvider).toContain("Retrieved scripture context:");
    expect(promptSeenByProvider).toContain("Bhagavad Gita 2.47");
  });

  it("requires auth for dev dashboard stats", async () => {
    const app = createApp({
      env: { ...env, adminEmails: ["admin@example.com"] },
      logger: false,
      authVerifier: { verifyIdToken: async () => ({ uid: "admin", email: "admin@example.com" }) },
      devDashboardStore: {
        readStats: async () => ({ totalUsers: 0, totalUserMessages: 0, users: [] })
      }
    });
    const response = await app.inject({ method: "GET", url: "/api/devdash/stats" });
    expect(response.statusCode).toBe(401);
  });

  it("rejects non-admin users from dev dashboard stats", async () => {
    const app = createApp({
      env: { ...env, adminEmails: ["admin@example.com"] },
      logger: false,
      authVerifier: { verifyIdToken: async () => ({ uid: "u1", email: "seeker@example.com" }) },
      devDashboardStore: {
        readStats: async () => ({ totalUsers: 0, totalUserMessages: 0, users: [] })
      }
    });
    const response = await app.inject({
      method: "GET",
      url: "/api/devdash/stats",
      headers: { authorization: "Bearer seeker-token" }
    });
    expect(response.statusCode).toBe(403);
  });

  it("returns aggregate dev dashboard stats for an allowlisted admin", async () => {
    const app = createApp({
      env: { ...env, adminEmails: ["admin@example.com"] },
      logger: false,
      authVerifier: { verifyIdToken: async () => ({ uid: "admin", email: "admin@example.com" }) },
      devDashboardStore: {
        readStats: async () => ({
          totalUsers: 1,
          totalUserMessages: 3,
          users: [{
            uid: "u1",
            email: "seeker@example.com",
            displayName: "Seeker",
            createdAt: "2026-05-20T10:00:00.000Z",
            lastSeenAt: "2026-05-20T11:00:00.000Z",
            messageCount: 3
          }]
        })
      }
    });
    const response = await app.inject({
      method: "GET",
      url: "/api/devdash/stats",
      headers: { authorization: "Bearer admin-token" }
    });
    expect(response.statusCode).toBe(200);
    expect(response.json()).toMatchObject({
      totalUsers: 1,
      totalUserMessages: 3,
      users: [{ uid: "u1", messageCount: 3 }]
    });
    expect(response.body).not.toContain("latestUserMessage");
  });
});
