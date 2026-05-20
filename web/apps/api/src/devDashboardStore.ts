import admin from "firebase-admin";
import type { DevDashboardStats, DevDashboardStore, DevDashboardUserStats } from "./app.js";
import type { ApiEnv } from "./env.js";
import { initializeFirebaseAdmin } from "./firebaseAdmin.js";

export function createFirebaseDevDashboardStore(env: ApiEnv): DevDashboardStore {
  return {
    async readStats(): Promise<DevDashboardStats> {
      if (!initializeFirebaseAdmin(env)) {
        throw new Error("Firebase Admin is required for the developer dashboard.");
      }

      const [authUsers, messageCounts] = await Promise.all([
        listAllAuthUsers(),
        readUserMessageCounts()
      ]);

      const users = authUsers.map((user): DevDashboardUserStats => ({
        uid: user.uid,
        email: user.email,
        displayName: user.displayName,
        createdAt: user.metadata.creationTime,
        lastSeenAt: messageCounts.get(user.uid)?.lastSeenAt ?? user.metadata.lastSignInTime,
        messageCount: messageCounts.get(user.uid)?.messageCount ?? 0
      }));

      const totalUserMessages = users.reduce((sum, user) => sum + user.messageCount, 0);

      return {
        totalUsers: users.length,
        totalUserMessages,
        users: users.sort((a, b) => b.messageCount - a.messageCount)
      };
    }
  };
}

async function listAllAuthUsers() {
  const users: admin.auth.UserRecord[] = [];
  let pageToken: string | undefined;
  do {
    const page = await admin.auth().listUsers(1000, pageToken);
    users.push(...page.users);
    pageToken = page.pageToken;
  } while (pageToken);
  return users;
}

async function readUserMessageCounts() {
  const counts = new Map<string, { messageCount: number; lastSeenAt?: string }>();
  const snapshot = await admin.firestore().collectionGroup("messages").get();

  snapshot.docs.forEach((message) => {
    const data = message.data();
    if (data.role !== "user") return;

    const pathParts = message.ref.path.split("/");
    const usersIndex = pathParts.indexOf("users");
    const uid = usersIndex >= 0 ? pathParts[usersIndex + 1] : undefined;
    if (!uid) return;

    const existing = counts.get(uid) ?? { messageCount: 0 };
    const createdAt = typeof data.createdAt === "string" ? data.createdAt : undefined;
    counts.set(uid, {
      messageCount: existing.messageCount + 1,
      lastSeenAt: maxIso(existing.lastSeenAt, createdAt)
    });
  });

  return counts;
}

function maxIso(left?: string, right?: string) {
  if (!left) return right;
  if (!right) return left;
  return right > left ? right : left;
}
