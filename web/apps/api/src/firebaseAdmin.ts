import admin from "firebase-admin";
import { createRemoteJWKSet, jwtVerify } from "jose";
import type { ApiEnv } from "./env.js";

export interface AuthVerifier {
  verifyIdToken(token: string): Promise<{ uid: string; name?: string; email?: string }>;
}

export function initializeFirebaseAdmin(env: ApiEnv) {
  if (admin.apps.length) return true;
  if (!env.firebaseProjectId || !env.firebaseClientEmail || !env.firebasePrivateKey) return false;

  admin.initializeApp({
    credential: admin.credential.cert({
      projectId: env.firebaseProjectId,
      clientEmail: env.firebaseClientEmail,
      privateKey: env.firebasePrivateKey
    })
  });

  return true;
}

export function createFirebaseAuthVerifier(env: ApiEnv): AuthVerifier {
  if (env.skipFirebaseAuth) {
    return {
      async verifyIdToken() {
        return { uid: "local-test-user", name: "friend" };
      }
    };
  }

  if (!env.firebaseProjectId) {
    throw new Error("FIREBASE_PROJECT_ID is required unless SKIP_FIREBASE_AUTH=true.");
  }

  if (initializeFirebaseAdmin(env)) {
    return {
      async verifyIdToken(token: string) {
        const decoded = await admin.auth().verifyIdToken(token);
        return { uid: decoded.uid, name: decoded.name, email: decoded.email };
      }
    };
  }

  const firebaseKeys = createRemoteJWKSet(
    new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com")
  );

  return {
    async verifyIdToken(token: string) {
      const { payload } = await jwtVerify(token, firebaseKeys, {
        issuer: `https://securetoken.google.com/${env.firebaseProjectId}`,
        audience: env.firebaseProjectId
      });
      if (!payload.sub) {
        throw new Error("Firebase ID token is missing subject.");
      }
      return {
        uid: payload.sub,
        name: typeof payload.name === "string" ? payload.name : undefined,
        email: typeof payload.email === "string" ? payload.email : undefined
      };
    }
  };
}
