import { createApp } from "./app.js";
import { createFirebaseDevDashboardStore } from "./devDashboardStore.js";
import { readEnv } from "./env.js";
import { createFirebaseAuthVerifier } from "./firebaseAdmin.js";

const env = readEnv();
const authVerifier = createFirebaseAuthVerifier(env);
const app = createApp({
  env,
  authVerifier,
  devDashboardStore: createFirebaseDevDashboardStore(env)
});

await app.listen({ host: "0.0.0.0", port: env.port });
