import { createApp } from "./app.js";
import { createFirebaseDevDashboardStore } from "./devDashboardStore.js";
import { readEnv } from "./env.js";
import { createFirebaseAuthVerifier } from "./firebaseAdmin.js";
import { createFileGuestUsageStore } from "./guestUsageStore.js";

const env = readEnv();
const authVerifier = createFirebaseAuthVerifier(env);
const app = createApp({
  env,
  authVerifier,
  devDashboardStore: createFirebaseDevDashboardStore(env),
  guestUsageStore: createFileGuestUsageStore(env.devdashStatsPath)
});

await app.listen({ host: "0.0.0.0", port: env.port });
