import { createDefaultUserMemory, type ChatMessage, type ShortTermMemory } from "@sarathi/shared-types";
import { getRedirectResult, onAuthStateChanged, signInWithPopup, signInWithRedirect, signOut, type User } from "firebase/auth";
import { KeyRound, LogIn, LogOut, MessageCircle, Send, Settings, Trash2 } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { auth, googleProvider } from "./firebase";
import { clearConversationHistory, ensureUserDefaults, getDevDashboardStats, getUserMemory, saveConversationMessage, saveUserMemory, type DevDashboardStats } from "./firestore";
import {
  clearGeminiApiKey,
  clearOpenRouterApiKey,
  getDemoClientId,
  readApiMode,
  readGeminiApiKey,
  readGuestSession,
  readOpenRouterApiKey,
  readProvider,
  saveApiMode,
  saveGeminiApiKey,
  saveGuestSession,
  saveOpenRouterApiKey,
  saveProvider,
  type AiProvider,
  type ApiMode
} from "./storage";
import "./styles.css";

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "/api";
const conversationId = "main";
const demoMessageLimit = 10;
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

function providerHasSavedKey(provider: AiProvider) {
  return Boolean((provider === "openrouter" ? readOpenRouterApiKey() : readGeminiApiKey()).trim());
}

function effectiveApiMode(user: User | null, provider: AiProvider): ApiMode {
  return user && readApiMode() === "user_key" && providerHasSavedKey(provider) ? "user_key" : "demo";
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
  const [apiMode, setApiMode] = useState<ApiMode>(() => effectiveApiMode(user, readProvider()));
  const [provider, setProvider] = useState<AiProvider>(() => readProvider());
  const [demoRemaining, setDemoRemaining] = useState(demoMessageLimit);
  const [ragDebug, setRagDebug] = useState<RagSearchDebug>({ used: false, sources: [] });

  useEffect(() => {
    const syncSettings = () => {
      const nextProvider = readProvider();
      setProvider(nextProvider);
      setApiMode(effectiveApiMode(user, nextProvider));
    };
    syncSettings();
    window.addEventListener("focus", syncSettings);
    return () => window.removeEventListener("focus", syncSettings);
  }, [user]);

  async function sendMessage(messageText = input) {
    const text = messageText.trim();
    if (!text || busy) return;
    const mode = user ? apiMode : "demo";
    const apiKey = provider === "openrouter" ? readOpenRouterApiKey() : readGeminiApiKey();
    if (mode === "user_key" && !apiKey) {
      setError("Please add your API key in Settings first.");
      return;
    }
    setError("");
    setBusy(true);
    const userMessage: ChatMessage = {
      id: crypto.randomUUID(),
      role: "user",
      text,
      createdAt: new Date().toISOString(),
      source: "frontend",
      provider: mode === "demo" ? "openrouter" : provider
    };
    const nextMessages = [...messages, userMessage];
    setMessages(nextMessages);
    setInput("");

    try {
      const longTermMemory = user ? await getUserMemory(user.uid) : null;
      const shortTermMemory = inferShortTermMemory(nextMessages);
      const data = await requestBackendSarathiResponse({
        mode,
        provider,
        apiKey,
        latestUserMessage: text,
        recentHistory: messages.slice(-8),
        shortTermMemory,
        longTermMemory,
        user
      });
      if ("demo" in data && data.demo) setDemoRemaining(data.demo.messagesRemaining);
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
    <section className="chat-layout">
      <div className="chat-panel">
        <div className="chat-heading">
          <div>
            <p className="eyebrow">{apiMode === "demo" || !user ? "Demo guidance through Google AI Studio" : `Using your ${provider === "gemini" ? "Google AI Studio" : "OpenRouter"} key`}</p>
            <h2>What rests upon your heart?</h2>
          </div>
          <button className="ghost" onClick={async () => {
            if (user) await clearConversationHistory(user.uid, conversationId);
            setMessages([]);
          }}><Trash2 size={17} /> Clear</button>
        </div>
        <div className="messages">
          {messages.length === 0 && (
            <>
              <div className="starter-prompts" aria-label="Suggested first messages">
                {starterPrompts.map((prompt) => (
                  <button key={prompt} onClick={() => sendMessage(prompt)} disabled={busy}>
                    {prompt}
                  </button>
                ))}
              </div>
              <article className="message assistant welcome-message">{welcomeMessage}</article>
            </>
          )}
          {messages.map((message) => (
            <article className={`message ${message.role}`} key={message.id}>
              {message.text}
            </article>
          ))}
          {busy && <article className="message assistant">The charioteer is reflecting...</article>}
        </div>
        {error && <p className="error">{error}</p>}
        {(apiMode === "demo" || !user) && (
          <div className="demo-banner">
            <span>Demo mode: Limited messages based on availability. Get your own API key for unlimited use.</span>
            <button onClick={() => navigate("/settings")}>Get Free Key</button>
          </div>
        )}
        <div className="references-panel">
          <strong>References:</strong>
          {ragDebug.sources.length > 0 ? (
            <ul>
              {ragDebug.sources.map((source) => <li key={source}>{source}</li>)}
            </ul>
          ) : (
            <span>N/A</span>
          )}
        </div>
        <div className="composer">
          <textarea
            value={input}
            onChange={(event) => setInput(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                sendMessage();
              }
            }}
            placeholder="What rests upon your heart?"
          />
          <button onClick={() => sendMessage()} disabled={busy || !input.trim()}><Send size={18} /></button>
        </div>
      </div>
    </section>
  );
}

async function requestBackendSarathiResponse(input: {
  mode: ApiMode;
  provider: AiProvider;
  apiKey: string;
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
      mode: input.mode,
      provider: input.provider,
      apiKey: input.mode === "user_key" ? input.apiKey : undefined,
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
  const [apiMode, setApiMode] = useState<ApiMode>(() => user ? readApiMode() : "demo");
  const [provider, setProviderState] = useState<AiProvider>(readProvider());
  const [apiKey, setApiKey] = useState(readGeminiApiKey());
  const [saved, setSaved] = useState(false);
  const masked = useMemo(() => apiKey ? `${apiKey.slice(0, 6)}...${apiKey.slice(-4)}` : "No key saved", [apiKey]);

  useEffect(() => {
    setApiKey(provider === "openrouter" ? readOpenRouterApiKey() : readGeminiApiKey());
  }, [provider]);

  function chooseMode(nextMode: ApiMode) {
    if (!user && nextMode === "user_key") {
      setApiMode("user_key");
      saveApiMode("user_key");
      return;
    }
    setApiMode(nextMode);
    saveApiMode(nextMode);
  }

  function chooseProvider(nextProvider: AiProvider) {
    setProviderState(nextProvider);
    saveProvider(nextProvider);
  }

  function saveKey() {
    if (provider === "openrouter") saveOpenRouterApiKey(apiKey.trim());
    else saveGeminiApiKey(apiKey.trim());
    saveApiMode("user_key");
    setSaved(true);
  }

  function forgetKey() {
    if (provider === "openrouter") clearOpenRouterApiKey();
    else clearGeminiApiKey();
    setApiKey("");
  }

  return (
    <section className="settings-grid">
      <div className="settings-panel">
        <p className="eyebrow">AI Provider</p>
        <h2>API key</h2>
        <p className="settings-note">Demo mode uses Sarathi's server key. Your own key is saved in this browser and sent to Sarathi's API only when you send a message. The server does not store it.</p>
        <div className="segmented-control" role="tablist" aria-label="API key mode">
          <button className={apiMode === "demo" ? "active" : ""} onClick={() => chooseMode("demo")}>Demo Mode</button>
          <button className={apiMode === "user_key" ? "active" : ""} onClick={() => chooseMode("user_key")}>Use My Key</button>
        </div>
        {apiMode === "demo" && <p className="provider-callout">Using server-provided AI key. No setup needed. Just start chatting.</p>}
        {apiMode === "user_key" && !user && (
          <div className="provider-callout">
            <p>Sign in with Google to use your own key.</p>
            <button className="primary-action small" onClick={() => signInWithGoogle(navigate, setAuthError)}>Sign in with Google</button>
            {authError && <p className="error inline-error">{authError}</p>}
          </div>
        )}
        {apiMode === "user_key" && user && (
          <>
            <label>
              <span>Provider</span>
              <select value={provider} onChange={(event) => chooseProvider(event.target.value as AiProvider)}>
                <option value="gemini">Google (AI Studio)</option>
                <option value="openrouter">OpenRouter</option>
              </select>
            </label>
            <label>
              <span>API Key</span>
              <input type="password" value={apiKey} onChange={(event) => setApiKey(event.target.value)} placeholder="Paste your key" />
            </label>
            <p className="key-links">Get your key: <a href="https://aistudio.google.com/apikey" target="_blank" rel="noreferrer">Google AI Studio</a> · <a href="https://openrouter.ai/keys" target="_blank" rel="noreferrer">OpenRouter</a></p>
            <p className="billing-note">For peace of mind, please use API keys without enabling billing, so Sarathi helps you avoid any unexpected charges.</p>
            <p className="muted"><KeyRound size={15} /> {masked}</p>
            <div className="button-row">
              <button className="primary-action small" onClick={saveKey}>Save key</button>
              <button className="ghost" onClick={forgetKey}>Forget key</button>
            </div>
            {saved && <p className="success">Saved in this browser. Sarathi's API will use this key only for your chat requests.</p>}
          </>
        )}
      </div>
      <div className="settings-panel">
        <p className="eyebrow">Memory</p>
        <h2>Long-term memory</h2>
        <p className="settings-note">Sarathi only saves personal notes when you clearly ask it to remember something. You can clear the memory here.</p>
        {user ? <button className="ghost danger" onClick={async () => {
          await saveUserMemory(user.uid, createDefaultUserMemory());
        }}><Trash2 size={17} /> Clear long-term memory</button> : <p className="settings-note">Guest chats do not save long-term memory.</p>}
      </div>
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
      <Route path="/" element={<Landing user={user} onContinueAsGuest={() => {
        setIsGuest(true);
        saveGuestSession(true);
        saveApiMode("demo");
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
