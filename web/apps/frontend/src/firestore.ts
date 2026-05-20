import type { ChatMessage, UserMemory, UserPreferences } from "@sarathi/shared-types";
import { createDefaultUserMemory, createDefaultUserPreferences } from "@sarathi/shared-types";
import {
  collection,
  collectionGroup,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  orderBy,
  query,
  setDoc,
  writeBatch
} from "firebase/firestore";
import { db } from "./firebase";

export interface DevDashboardStats {
  totalUsers: number;
  totalUserMessages: number;
  users: Array<{
    uid: string;
    email?: string;
    displayName?: string;
    createdAt?: string;
    lastSeenAt?: string;
    messageCount: number;
  }>;
}

export async function ensureUserDefaults(uid: string, displayName?: string | null) {
  const now = new Date().toISOString();
  await setDoc(doc(db, "users", uid, "profile", "main"), { displayName: displayName ?? "friend", updatedAt: now }, { merge: true });
  await setDoc(doc(db, "users", uid, "preferences", "main"), createDefaultUserPreferences(now), { merge: true });
  await setDoc(doc(db, "users", uid, "memory", "main"), createDefaultUserMemory(now), { merge: true });
}

export async function getUserMemory(uid: string): Promise<UserMemory> {
  const snapshot = await getDoc(doc(db, "users", uid, "memory", "main"));
  return snapshot.exists() ? (snapshot.data() as UserMemory) : createDefaultUserMemory();
}

export async function saveUserMemory(uid: string, memory: UserMemory) {
  await setDoc(doc(db, "users", uid, "memory", "main"), memory, { merge: true });
}

export async function savePreferences(uid: string, preferences: UserPreferences) {
  await setDoc(doc(db, "users", uid, "preferences", "main"), preferences, { merge: true });
}

export async function saveConversationMessage(uid: string, conversationId: string, message: ChatMessage) {
  const now = new Date().toISOString();
  const conversationRef = doc(db, "users", uid, "conversations", conversationId);
  await setDoc(conversationRef, {
    title: "Guidance",
    createdAt: now,
    updatedAt: now,
    lastMessagePreview: message.text.slice(0, 120),
    modelProvider: message.provider === "openrouter" ? "openrouter" : "gemini",
    messageCount: 0,
    archived: false
  }, { merge: true });
  await setDoc(doc(collection(conversationRef, "messages"), message.id), message);
}

export async function loadConversationMessages(uid: string, conversationId: string): Promise<ChatMessage[]> {
  const messages = await getDocs(query(collection(db, "users", uid, "conversations", conversationId, "messages"), orderBy("createdAt", "asc")));
  return messages.docs.map((message) => message.data() as ChatMessage);
}

export async function clearConversationHistory(uid: string, conversationId: string) {
  const messages = await getDocs(collection(db, "users", uid, "conversations", conversationId, "messages"));
  const batch = writeBatch(db);
  messages.docs.forEach((message) => batch.delete(message.ref));
  await batch.commit();
  await deleteDoc(doc(db, "users", uid, "conversations", conversationId));
}

export async function getDevDashboardStats(): Promise<DevDashboardStats> {
  const [profilesSnapshot, messagesSnapshot] = await Promise.all([
    getDocs(collectionGroup(db, "profile")),
    getDocs(collectionGroup(db, "messages"))
  ]);

  const users = new Map<string, DevDashboardStats["users"][number]>();

  profilesSnapshot.docs.forEach((profile) => {
    const uid = uidFromPath(profile.ref.path);
    if (!uid) return;
    const data = profile.data() as { displayName?: string; email?: string; createdAt?: string; updatedAt?: string };
    users.set(uid, {
      uid,
      email: data.email,
      displayName: data.displayName,
      createdAt: data.createdAt ?? data.updatedAt,
      lastSeenAt: data.updatedAt ?? data.createdAt,
      messageCount: 0
    });
  });

  messagesSnapshot.docs.forEach((message) => {
    const data = message.data() as { role?: string; createdAt?: string };
    if (data.role !== "user") return;
    const uid = uidFromPath(message.ref.path);
    if (!uid) return;
    const existing = users.get(uid) ?? { uid, messageCount: 0 };
    users.set(uid, {
      ...existing,
      messageCount: existing.messageCount + 1,
      lastSeenAt: maxIso(existing.lastSeenAt, data.createdAt)
    });
  });

  const userList = [...users.values()].sort((left, right) => right.messageCount - left.messageCount);
  return {
    totalUsers: userList.length,
    totalUserMessages: userList.reduce((sum, user) => sum + user.messageCount, 0),
    users: userList
  };
}

function uidFromPath(path: string) {
  const parts = path.split("/");
  const usersIndex = parts.indexOf("users");
  return usersIndex >= 0 ? parts[usersIndex + 1] : undefined;
}

function maxIso(left?: string, right?: string) {
  if (!left) return right;
  if (!right) return left;
  return right > left ? right : left;
}
