import { DEFAULT_GEMINI_MODEL, PRODUCTION_ORIGIN } from "@sarathi/shared-config";

export interface ApiEnv {
  port: number;
  allowedOrigin: string;
  geminiModel: string;
  openRouterDemoApiKey?: string;
  openRouterDemoModel: string;
  openRouterUserModel: string;
  demoMessageLimit: number;
  adminEmails: string[];
  adminUids: string[];
  firebaseProjectId?: string;
  firebaseClientEmail?: string;
  firebasePrivateKey?: string;
  skipFirebaseAuth: boolean;
}

export function readEnv(source = process.env): ApiEnv {
  return {
    port: Number(source.PORT ?? 3001),
    allowedOrigin: source.ALLOWED_ORIGIN ?? PRODUCTION_ORIGIN,
    geminiModel: source.GEMINI_MODEL ?? DEFAULT_GEMINI_MODEL,
    openRouterDemoApiKey: source.OPENROUTER_DEMO_API_KEY,
    openRouterDemoModel: source.OPENROUTER_DEMO_MODEL ?? "openrouter/free",
    openRouterUserModel: source.OPENROUTER_USER_MODEL ?? source.OPENROUTER_DEMO_MODEL ?? "openrouter/free",
    demoMessageLimit: Number(source.DEMO_MESSAGE_LIMIT ?? 10),
    adminEmails: parseList(source.DEVDASH_ADMIN_EMAILS),
    adminUids: parseList(source.DEVDASH_ADMIN_UIDS),
    firebaseProjectId: source.FIREBASE_PROJECT_ID,
    firebaseClientEmail: source.FIREBASE_CLIENT_EMAIL,
    firebasePrivateKey: source.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, "\n"),
    skipFirebaseAuth: source.SKIP_FIREBASE_AUTH === "true"
  };
}

function parseList(value?: string) {
  return (value ?? "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}
