import cors from "@fastify/cors";
import { buildSarathiPrompt, normalizeSarathiResponse } from "@sarathi/shared-persona";
import type { ChatMessage, ShortTermMemory, UserMemory } from "@sarathi/shared-types";
import Fastify, { type FastifyBaseLogger, type FastifyReply, type FastifyRequest } from "fastify";
import { z } from "zod";
import type { ApiEnv } from "./env.js";
import type { AuthVerifier } from "./firebaseAdmin.js";
import { generateSarathiResponse } from "./gemini.js";
import type { GuestUsageStore } from "./guestUsageStore.js";
import { searchBackendRagPassages } from "./rag.js";

type SarathiCloudProvider = "gemini" | "deepseek" | "openrouter";

const chatRequestSchema = z.object({
  mode: z.enum(["demo", "user_key"]).optional(),
  provider: z.enum(["gemini", "deepseek", "openrouter"]).optional(),
  apiKey: z.string().min(10).optional(),
  geminiApiKey: z.string().min(10).optional(),
  demoClientId: z.string().min(8).max(128).optional(),
  conversationId: z.string().min(1),
  latestUserMessage: z.string().min(1).max(8000),
  recentHistory: z.array(z.custom<ChatMessage>()).default([]),
  shortTermMemory: z.custom<ShortTermMemory>().optional(),
  longTermMemory: z.custom<UserMemory>().nullable().optional()
});

export interface CreateAppOptions {
  env: ApiEnv;
  authVerifier: AuthVerifier;
  logger?: boolean | FastifyBaseLogger;
  generateResponse?: typeof generateSarathiResponse;
  devDashboardStore?: DevDashboardStore;
  guestUsageStore?: GuestUsageStore;
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

async function requireDevDashboardAdmin(
  request: FastifyRequest,
  reply: FastifyReply,
  options: CreateAppOptions
) {
  const authHeader = request.headers.authorization;
  const token = authHeader?.startsWith("Bearer ") ? authHeader.slice("Bearer ".length) : null;
  if (!token) {
    reply.code(401).send({ error: "Missing Firebase ID token." });
    return null;
  }

  const user = await options.authVerifier.verifyIdToken(token);
  const emailAllowed = user.email ? options.env.adminEmails.includes(user.email) : false;
  const uidAllowed = options.env.adminUids.includes(user.uid);
  if (!emailAllowed && !uidAllowed) {
    reply.code(403).send({ error: "Developer dashboard access denied." });
    return null;
  }

  return user;
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
    provider: "server-managed",
    model: options.env.geminiModel,
    providers: buildProviderCascade(options).map((provider) => ({
      provider: provider.provider,
      model: provider.model
    }))
  }));

  app.get("/api/devdash/stats", async (request, reply) => {
    const adminUser = await requireDevDashboardAdmin(request, reply, options);
    if (!adminUser) return;

    if (!options.devDashboardStore) {
      return reply.code(503).send({ error: "Developer dashboard is not configured." });
    }

    return options.devDashboardStore.readStats();
  });

  app.get("/api/devdash/guest-stats", async (request, reply) => {
    const adminUser = await requireDevDashboardAdmin(request, reply, options);
    if (!adminUser) return;

    if (!options.guestUsageStore) {
      return { totalGuestMessages: 0 };
    }

    return options.guestUsageStore.readGuestStats();
  });

  app.post("/api/chat", async (request, reply) => {
    const body = chatRequestSchema.parse(request.body);
    const authHeader = request.headers.authorization;
    const token = authHeader?.startsWith("Bearer ") ? authHeader.slice("Bearer ".length) : null;
    const user = token ? await options.authVerifier.verifyIdToken(token) : { uid: body.demoClientId ?? request.ip, name: "friend" };
    const isGuest = !token;
    const providers = buildProviderCascade(options);

    if (providers.length === 0) {
      return reply.code(503).send({ error: "Sarathi online guidance is not configured." });
    }

    let messagesRemaining: number | undefined;
    if (isGuest) {
      const limit = Math.max(0, options.env.demoMessageLimit);
      const usageKey = user.uid || body.demoClientId || request.ip;
      const used = demoUsage.get(usageKey) ?? 0;
      if (used >= limit) {
        return reply.code(429).send({ error: "Demo message limit reached.", demo: { messagesRemaining: 0, messageLimit: limit } });
      }
      demoUsage.set(usageKey, used + 1);
      messagesRemaining = Math.max(0, limit - used - 1);
    }

    const ragPassages = searchBackendRagPassages(body.latestUserMessage, 5);

    const prompt = buildSarathiPrompt({
      userName: user.name,
      latestUserMessage: body.latestUserMessage,
      recentHistory: body.recentHistory,
      ragPassages,
      shortTermMemory: body.shortTermMemory,
      longTermMemory: body.longTermMemory
    });

    let generated: Awaited<ReturnType<typeof generateResponse>> | undefined;
    let selectedProvider = providers[0];
    let lastError: unknown;
    for (const candidate of providers) {
      try {
        generated = await generateResponse({
          apiKey: candidate.apiKey,
          model: candidate.model,
          provider: candidate.provider,
          prompt
        });
        selectedProvider = candidate;
        lastError = undefined;
        break;
      } catch (error) {
        lastError = error;
        request.log.warn({ err: error, provider: candidate.provider }, "server-managed provider failed");
      }
    }

    if (lastError || !generated) {
      throw lastError;
    }

    if (isGuest) {
      await options.guestUsageStore?.incrementGuestMessages();
    }

    return {
      assistantMessage: normalizeSarathiResponse({
        responseText: generated.text,
        userMessage: body.latestUserMessage,
        ragPassages
      }),
      provider: selectedProvider.provider,
      model: selectedProvider.model,
      rag: {
        used: ragPassages.length > 0,
        sources: ragPassages.map((passage) => passage.citation)
      },
      demo: isGuest ? { messagesRemaining, messageLimit: options.env.demoMessageLimit } : undefined,
      diagnostics: generated.usage ? { usage: generated.usage } : undefined
    };
  });

  return app;
}

function buildProviderCascade(options: CreateAppOptions): Array<{
  provider: SarathiCloudProvider;
  apiKey: string;
  model: string;
}> {
  return [
    options.env.geminiDemoApiKey
      ? { provider: "gemini" as const, apiKey: options.env.geminiDemoApiKey, model: options.env.geminiModel }
      : null,
    options.env.deepSeekDemoApiKey
      ? { provider: "deepseek" as const, apiKey: options.env.deepSeekDemoApiKey, model: options.env.deepSeekDemoModel }
      : null,
    options.env.openRouterDemoApiKey
      ? { provider: "openrouter" as const, apiKey: options.env.openRouterDemoApiKey, model: options.env.openRouterDemoModel }
      : null
  ].filter((provider): provider is {
    provider: SarathiCloudProvider;
    apiKey: string;
    model: string;
  } => provider !== null);
}
