import { createDefaultUserMemory, type ChatMessage, type ShortTermMemory } from "@sarathi/shared-types";
import { getRedirectResult, onAuthStateChanged, signInWithPopup, signInWithRedirect, signOut, type User } from "firebase/auth";
import { LogIn, LogOut, MessageCircle, Settings, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { auth, googleProvider } from "./firebase";
import { ensureUserDefaults, getDevDashboardStats, getUserMemory, saveConversationMessage, saveUserMemory, type DevDashboardStats } from "./firestore";
import { SarathiChatPanel } from "./components/sarathi/SarathiChatPanel";
import { SoulMemorySettings } from "./SoulMemorySettings";
import {
  MockDesignIndex,
  SarathiDesignOnePage,
  SarathiDesignThreePage,
  SarathiDesignTwoPage,
  SarathiDesignTwoSignedInPage,
} from "./sarathiDesigns";
import {
  dismissSignInPersonalizationBanner,
  getDemoClientId,
  readGuestSession,
  readSignInPersonalizationBannerDismissed,
  saveGuestSession,
} from "./storage";
import "./styles.css";

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "/api";
const conversationId = "main";
const postLoginRouteStorage = "sarathi.postLoginRoute";
const welcomeMessage = "My dear one,\n\nI have been seated quietly in the chariot of your heart.\n\nTell me \u2014 what battle stands before you today?";
const starterPrompts = [
  "I feel anxious",
  "I need clarity",
  "I am attached to an outcome",
  "I failed at something",
  "I want to understand my dharma",
  "Teach me a verse"
];

interface RagSearchDebug {
  used: boolean;
  sources: string[];
}

async function signInWithGoogle(navigate: ReturnType<typeof useNavigate>, setAuthError?: (message: string) => void, targetPath = "/chat") {
  setAuthError?.("");
  try {
    const result = await signInWithPopup(auth, googleProvider);
    await ensureUserDefaults(result.user.uid, result.user.displayName);
    navigate(targetPath);
  } catch (error) {
    const code = typeof error === "object" && error && "code" in error ? String(error.code) : "";
    if (code === "auth/popup-blocked" || code === "auth/popup-closed-by-user") {
      sessionStorage.setItem(postLoginRouteStorage, targetPath);
      await signInWithRedirect(auth, googleProvider);
      return;
    }
    setAuthError?.("Google sign-in is not ready yet. Please check Firebase authorized domains.");
  }
}

function AppShell({ user, isGuest, onExitGuest, children }: { user: User | null; isGuest: boolean; onExitGuest: () => void; children: React.ReactNode }) {
  const navigate = useNavigate();
  return (
    <div className="app-shell">
      <header className="topbar">
        <Link to="/chat" className="brand"><img src="/brand/sarathi-logo.png" alt="" /> Sarathi</Link>
        <nav>
          <Link to="/chat"><MessageCircle size={18} /> Chat</Link>
          <Link to="/settings"><Settings size={18} /> Settings</Link>
          {user ? (
            <button onClick={() => signOut(auth)}><LogOut size={18} /> Sign out</button>
          ) : (
            <>
              <button onClick={() => signInWithGoogle(navigate)}><LogIn size={18} /> Sign in</button>
              {isGuest && <button onClick={onExitGuest}><LogOut size={18} /> Exit guest</button>}
            </>
          )}
        </nav>
      </header>
      <main>{children}</main>
      <span className="user-chip">{user ? user.displayName || user.email : "Guest demo mode"}</span>
    </div>
  );
}

function Landing({ user, onContinueAsGuest }: { user: User | null; onContinueAsGuest: () => void }) {
  const navigate = useNavigate();
  const [authError, setAuthError] = useState("");
  if (user) return <Navigate to="/chat" replace />;
  return (
    <main className="landing">
      <section className="landing-copy">
        <img className="landing-lotus" src="/brand/splash-lotus-icon.png" alt="" />
        <h1>Sarathi</h1>
        <div className="landing-logo-wrap">
          <span className="logo-particle p1" aria-hidden="true" />
          <span className="logo-particle p2" aria-hidden="true" />
          <span className="logo-particle p3" aria-hidden="true" />
          <span className="logo-particle p4" aria-hidden="true" />
          <span className="logo-particle p5" aria-hidden="true" />
          <span className="logo-particle p6" aria-hidden="true" />
          <span className="logo-particle p7" aria-hidden="true" />
          <span className="logo-particle p8" aria-hidden="true" />
          <img className="landing-hero-flute" src="/brand/splash-hero-flute.png" alt="" />
          <img className="landing-hero-feather shadow" src="/brand/splash-hero-feather.png" alt="" />
          <img className="landing-hero-feather" src="/brand/splash-hero-feather.png" alt="Sarathi" />
        </div>
        <p className="subcopy">When the heart grows quiet,<br />the charioteer speaks.</p>
        <img className="landing-divider" src="/brand/splash-divider-lotus.png" alt="" />
        <p className="welcome-line">Welcome, dear one.</p>
        <div className="landing-actions">
          <button className="primary-action" onClick={() => signInWithGoogle(navigate, setAuthError)}>Continue with Google</button>
          <button className="ghost guest-action" onClick={() => {
            onContinueAsGuest();
            navigate("/chat");
          }}>Continue as guest</button>
        </div>
        {authError && <p className="error landing-error">{authError}</p>}
      </section>
    </main>
  );
}

function inferShortTermMemory(messages: ChatMessage[]): ShortTermMemory {
  const lastUserText = [...messages].reverse().find((message) => message.role === "user")?.text ?? "";
  return {
    currentConcern: lastUserText.slice(0, 180),
    preferredGuidanceStyle: "simple, calm, practical",
    lastGuidanceTheme: "Gita-inspired steadiness"
  };
}

function Chat({ user }: { user: User | null }) {
  const navigate = useNavigate();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [signInBannerDismissed, setSignInBannerDismissed] = useState(readSignInPersonalizationBannerDismissed);
  const [ragDebug, setRagDebug] = useState<RagSearchDebug>({ used: false, sources: [] });

  async function sendMessage(messageText = input) {
    const text = messageText.trim();
    if (!text || busy) return;
    setError("");
    setBusy(true);
    const userMessage: ChatMessage = {
      id: crypto.randomUUID(),
      role: "user",
      text,
      createdAt: new Date().toISOString(),
      source: "frontend"
    };
    const nextMessages = [...messages, userMessage];
    setMessages(nextMessages);
    setInput("");

    try {
      const longTermMemory = user ? await getUserMemory(user.uid) : null;
      const shortTermMemory = inferShortTermMemory(nextMessages);
      const data = await requestBackendSarathiResponse({
        latestUserMessage: text,
        recentHistory: messages.slice(-8),
        shortTermMemory,
        longTermMemory,
        user
      });
      if ("rag" in data && data.rag) setRagDebug(data.rag);
      const assistantMessage: ChatMessage = {
        id: crypto.randomUUID(),
        role: "assistant",
        text: data.assistantMessage,
        createdAt: new Date().toISOString(),
        source: "api",
        provider: data.provider,
        memoryUsed: true
      };
      setMessages([...nextMessages, assistantMessage]);
      if (user) {
        await saveConversationMessage(user.uid, conversationId, userMessage);
        await saveConversationMessage(user.uid, conversationId, assistantMessage);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went quiet. Please try again.");
      setMessages(messages);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="production-chat-layout">
      <SarathiChatPanel
        messages={messages}
        prompts={starterPrompts}
        welcomeMessage={welcomeMessage}
        input={input}
        busy={busy}
        error={error}
        references={ragDebug.sources}
        showMemoryCard={!user && !signInBannerDismissed}
        signedIn={Boolean(user)}
        composerPlaceholder="Share what rests upon your heart..."
        onPrompt={sendMessage}
        onClear={() => setMessages([])}
        onInputChange={setInput}
        onSubmit={() => sendMessage()}
        onSignIn={() => signInWithGoogle(navigate)}
        onDismissMemory={() => {
          dismissSignInPersonalizationBanner();
          setSignInBannerDismissed(true);
        }}
      />
    </section>
  );
}

async function requestBackendSarathiResponse(input: {
  latestUserMessage: string;
  recentHistory: ChatMessage[];
  shortTermMemory: ShortTermMemory;
  longTermMemory: Awaited<ReturnType<typeof getUserMemory>> | null;
  user: User | null;
}): Promise<{
  assistantMessage: string;
  provider?: string;
  rag?: RagSearchDebug;
  demo?: { messagesRemaining: number; messageLimit: number };
}> {
  const token = input.user ? await input.user.getIdToken() : null;
  const response = await fetch(`${apiBaseUrl}/chat`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify({
      demoClientId: getDemoClientId(),
      conversationId,
      latestUserMessage: input.latestUserMessage,
      recentHistory: input.recentHistory,
      shortTermMemory: input.shortTermMemory,
      longTermMemory: input.longTermMemory
    })
  });
  const data = await response.json() as {
    assistantMessage?: string;
    provider?: string;
    rag?: RagSearchDebug;
    demo?: { messagesRemaining: number; messageLimit: number };
    error?: string;
  };
  if (!response.ok || !data.assistantMessage) throw new Error(data.error || "Sarathi could not answer right now.");
  return {
    assistantMessage: data.assistantMessage,
    provider: data.provider,
    rag: data.rag,
    demo: data.demo
  };
}

function SettingsPage({ user }: { user: User | null }) {
  const navigate = useNavigate();
  const [authError, setAuthError] = useState("");

  return (
    <section className="settings-page">
      <div className="settings-grid">
        <div className="settings-panel">
          <p className="eyebrow">Memory</p>
          <h2>Personal guidance</h2>
          <p className="settings-note">
            Sign in to let Sarathi keep a gentle memory file for notes you explicitly ask it to remember.
          </p>
          {user ? (
            <button className="ghost danger" onClick={async () => {
              await saveUserMemory(user.uid, createDefaultUserMemory());
            }}><Trash2 size={17} /> Clear long-term memory</button>
          ) : (
            <>
              <button className="primary-action small" onClick={() => signInWithGoogle(navigate, setAuthError)}>
                <LogIn size={17} /> Sign in for personal memory
              </button>
              {authError && <p className="error inline-error">{authError}</p>}
            </>
          )}
        </div>
      </div>
      <SoulMemorySettings />
    </section>
  );
}

function DevDashboard({ user }: { user: User | null }) {
  const navigate = useNavigate();
  const [authError, setAuthError] = useState("");
  const [stats, setStats] = useState<DevDashboardStats | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!user) return;
    const signedInUser = user;
    let cancelled = false;
    async function loadStats() {
      setLoading(true);
      setError("");
      try {
        await signedInUser.getIdToken();
        const data = await getDevDashboardStats();
        if (!cancelled) setStats(data as DevDashboardStats);
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Developer dashboard is not available.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    loadStats();
    return () => {
      cancelled = true;
    };
  }, [user]);

  if (!user) {
    return (
      <section className="devdash-layout">
        <div className="settings-panel">
          <p className="eyebrow">Developer Dashboard</p>
          <h2>Sign in required</h2>
          <p className="settings-note">This dashboard is restricted to approved Sarathi administrators.</p>
          <button className="primary-action small" onClick={() => signInWithGoogle(navigate, setAuthError, "/devdash")}>Sign in with Google</button>
          {authError && <p className="error inline-error">{authError}</p>}
        </div>
      </section>
    );
  }

  return (
    <section className="devdash-layout">
      <div className="settings-panel">
        <p className="eyebrow">Developer Dashboard</p>
        <h2>Sarathi usage</h2>
        {loading && <p className="settings-note">Loading dashboard...</p>}
        {error && <p className="error inline-error">{error}</p>}
        {stats && (
          <>
            <div className="metric-grid">
              <div className="metric-card">
                <span>Registered users</span>
                <strong>{stats.totalUsers}</strong>
              </div>
              <div className="metric-card">
                <span>User messages</span>
                <strong>{stats.totalUserMessages}</strong>
              </div>
            </div>
            <div className="devdash-table-wrap">
              <table className="devdash-table">
                <thead>
                  <tr>
                    <th>User</th>
                    <th>Messages</th>
                    <th>Last seen</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {stats.users.map((entry) => (
                    <tr key={entry.uid}>
                      <td>
                        <strong>{entry.displayName || entry.email || entry.uid}</strong>
                        <span>{entry.email || entry.uid}</span>
                      </td>
                      <td>{entry.messageCount}</td>
                      <td>{formatDate(entry.lastSeenAt)}</td>
                      <td>{formatDate(entry.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>
    </section>
  );
}

function formatDate(value?: string) {
  if (!value) return "N/A";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

export function App() {
  const navigate = useNavigate();
  const [user, setUser] = useState<User | null>(null);
  const [isGuest, setIsGuest] = useState(() => readGuestSession());
  const [ready, setReady] = useState(false);

  useEffect(() => onAuthStateChanged(auth, async (nextUser) => {
    setUser(nextUser);
    if (nextUser) {
      setIsGuest(false);
      saveGuestSession(false);
      await ensureUserDefaults(nextUser.uid, nextUser.displayName);
    }
    setReady(true);
  }), []);

  useEffect(() => {
    const readyFallback = window.setTimeout(() => setReady(true), 4000);
    getRedirectResult(auth)
      .then(async (result) => {
        if (result?.user) {
          await ensureUserDefaults(result.user.uid, result.user.displayName);
          setUser(result.user);
          setIsGuest(false);
          saveGuestSession(false);
          const targetPath = sessionStorage.getItem(postLoginRouteStorage);
          sessionStorage.removeItem(postLoginRouteStorage);
          if (targetPath) navigate(targetPath);
        }
        setReady(true);
      })
      .catch(() => {
        setReady(true);
      })
      .finally(() => {
        window.clearTimeout(readyFallback);
      });
    return () => window.clearTimeout(readyFallback);
  }, [navigate]);

  if (!ready) return <main className="landing"><p className="empty-state">The charioteer is reflecting...</p></main>;

  return (
    <Routes>
      <Route path="/mock/sarathi-designs" element={<MockDesignIndex />} />
      <Route path="/mock/sarathi-design-1" element={<SarathiDesignOnePage />} />
      <Route path="/mock/sarathi-design-2" element={<SarathiDesignTwoPage />} />
      <Route path="/mock/sarathi-design-2-signed-in" element={<SarathiDesignTwoSignedInPage />} />
      <Route path="/mock/sarathi-design-3" element={<SarathiDesignThreePage />} />
      <Route path="/" element={<Landing user={user} onContinueAsGuest={() => {
        setIsGuest(true);
        saveGuestSession(true);
        getDemoClientId();
      }} />} />
      <Route path="/chat" element={(user || isGuest) ? <AppShell user={user} isGuest={isGuest} onExitGuest={() => {
        setIsGuest(false);
        saveGuestSession(false);
      }}><Chat user={user} /></AppShell> : <Navigate to="/" replace />} />
      <Route path="/settings" element={(user || isGuest) ? <AppShell user={user} isGuest={isGuest} onExitGuest={() => {
        setIsGuest(false);
        saveGuestSession(false);
      }}><SettingsPage user={user} /></AppShell> : <Navigate to="/" replace />} />
      <Route path="/devdash" element={<AppShell user={user} isGuest={isGuest} onExitGuest={() => {
        setIsGuest(false);
        saveGuestSession(false);
      }}><DevDashboard user={user} /></AppShell>} />
    </Routes>
  );
}
