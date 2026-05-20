import cors from "@fastify/cors";
import { buildSarathiPrompt, normalizeSarathiResponse } from "@sarathi/shared-persona";
import type { ChatMessage, RagPassage, ShortTermMemory, UserMemory } from "@sarathi/shared-types";
import Fastify, { type FastifyBaseLogger } from "fastify";
import { z } from "zod";
import type { ApiEnv } from "./env.js";
import type { AuthVerifier } from "./firebaseAdmin.js";
import { generateSarathiResponse } from "./gemini.js";

const providerSchema = z.enum(["gemini", "openrouter"]);

const chatRequestSchema = z.object({
  mode: z.enum(["demo", "user_key"]).default("user_key"),
  provider: providerSchema.default("gemini"),
  apiKey: z.string().min(10).optional(),
  geminiApiKey: z.string().min(10).optional(),
  demoClientId: z.string().min(8).max(128).optional(),
  conversationId: z.string().min(1),
  latestUserMessage: z.string().min(1).max(8000),
  recentHistory: z.array(z.custom<ChatMessage>()).default([]),
  ragPassages: z.array(z.custom<RagPassage>()).max(5).default([]),
  shortTermMemory: z.custom<ShortTermMemory>().optional(),
  longTermMemory: z.custom<UserMemory>().nullable().optional()
});

export interface CreateAppOptions {
  env: ApiEnv;
  authVerifier: AuthVerifier;
  logger?: boolean | FastifyBaseLogger;
  generateResponse?: typeof generateSarathiResponse;
  devDashboardStore?: DevDashboardStore;
}

export interface DevDashboardUserStats {
  uid: string;
  email?: string;
  displayName?: string;
  createdAt?: string;
  lastSeenAt?: string;
  messageCount: number;
}

export interface DevDashboardStats {
  totalUsers: number;
  totalUserMessages: number;
  users: DevDashboardUserStats[];
}

export interface DevDashboardStore {
  readStats(): Promise<DevDashboardStats>;
}

function redactApiKey(payload: unknown) {
  if (payload && typeof payload === "object") {
    const redacted = { ...(payload as Record<string, unknown>) };
    if ("geminiApiKey" in redacted) redacted.geminiApiKey = "[redacted]";
    if ("apiKey" in redacted) redacted.apiKey = "[redacted]";
    return redacted;
  }
  return payload;
}

export function createApp(options: CreateAppOptions) {
  const generateResponse = options.generateResponse ?? generateSarathiResponse;
  const demoUsage = new Map<string, number>();
  const app = Fastify({
    logger: options.logger ?? true
  });

  app.addHook("preHandler", async (request) => {
    if (request.body && typeof request.body === "object" && ("geminiApiKey" in request.body || "apiKey" in request.body)) {
      request.log.debug({ body: redactApiKey(request.body) }, "chat request body redacted");
    }
  });

  app.register(cors, {
    origin(origin, cb) {
      if (!origin || origin === options.env.allowedOrigin || origin.startsWith("http://localhost:")) {
        cb(null, true);
        return;
      }
      cb(new Error("Origin not allowed"), false);
    },
    credentials: true
  });

  app.get("/api/health", async () => ({
    ok: true,
    service: "sarathi-api",
    provider: "gemini",
    model: options.env.geminiModel,
    demoProvider: "openrouter",
    demoModel: options.env.openRouterDemoModel
  }));

  app.get("/api/devdash/stats", async (request, reply) => {
    const authHeader = request.headers.authorization;
    const token = authHeader?.startsWith("Bearer ") ? authHeader.slice("Bearer ".length) : null;
    if (!token) {
      return reply.code(401).send({ error: "Missing Firebase ID token." });
    }

    const user = await options.authVerifier.verifyIdToken(token);
    const emailAllowed = user.email ? options.env.adminEmails.includes(user.email) : false;
    const uidAllowed = options.env.adminUids.includes(user.uid);
    if (!emailAllowed && !uidAllowed) {
      return reply.code(403).send({ error: "Developer dashboard access denied." });
    }

    if (!options.devDashboardStore) {
      return reply.code(503).send({ error: "Developer dashboard is not configured." });
    }

    return options.devDashboardStore.readStats();
  });

  app.post("/api/chat", async (request, reply) => {
    const body = chatRequestSchema.parse(request.body);
    const authHeader = request.headers.authorization;
    const token = authHeader?.startsWith("Bearer ") ? authHeader.slice("Bearer ".length) : null;
    if (body.mode === "user_key" && !token) {
      return reply.code(401).send({ error: "Missing Firebase ID token." });
    }

    const user = token ? await options.authVerifier.verifyIdToken(token) : { uid: body.demoClientId ?? request.ip, name: "friend" };
    const isDemo = body.mode === "demo";
    const provider = isDemo ? "openrouter" : body.provider;
    const apiKey = isDemo ? options.env.openRouterDemoApiKey : body.apiKey ?? body.geminiApiKey;
    const model = isDemo
      ? options.env.openRouterDemoModel
      : provider === "openrouter"
        ? options.env.openRouterUserModel
        : options.env.geminiModel;

    if (!apiKey) {
      return reply.code(isDemo ? 503 : 400).send({ error: isDemo ? "Demo mode is not configured." : "Missing API key." });
    }

    let messagesRemaining: number | undefined;
    if (isDemo) {
      const limit = Math.max(0, options.env.demoMessageLimit);
      const usageKey = user.uid || body.demoClientId || request.ip;
      const used = demoUsage.get(usageKey) ?? 0;
      if (used >= limit) {
        return reply.code(429).send({ error: "Demo message limit reached.", demo: { messagesRemaining: 0, messageLimit: limit } });
      }
      demoUsage.set(usageKey, used + 1);
      messagesRemaining = Math.max(0, limit - used - 1);
    }

    const prompt = buildSarathiPrompt({
      userName: user.name,
      latestUserMessage: body.latestUserMessage,
      recentHistory: body.recentHistory,
      ragPassages: body.ragPassages,
      shortTermMemory: body.shortTermMemory,
      longTermMemory: body.longTermMemory
    });

    const generated = await generateResponse({
      apiKey,
      model,
      provider,
      prompt
    });

    return {
      assistantMessage: normalizeSarathiResponse({
        responseText: generated.text,
        userMessage: body.latestUserMessage,
        ragPassages: body.ragPassages
      }),
      provider,
      model,
      rag: {
        used: body.ragPassages.length > 0,
        sources: body.ragPassages.map((passage) => passage.citation)
      },
      demo: isDemo ? { messagesRemaining, messageLimit: options.env.demoMessageLimit } : undefined,
      diagnostics: generated.usage ? { usage: generated.usage } : undefined
    };
  });

  return app;
}
