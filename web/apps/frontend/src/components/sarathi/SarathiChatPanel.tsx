import type { ChatMessage } from "@sarathi/shared-types";
import {
  ArrowRight,
  BookOpen,
  Check,
  Send,
  ShieldCheck,
  Sparkles,
  Sun,
  Trash2,
  User,
} from "lucide-react";
import { Link } from "react-router-dom";

export type SarathiChatPanelProps = {
  messages: ChatMessage[];
  prompts: string[];
  welcomeMessage: string;
  input?: string;
  busy?: boolean;
  error?: string;
  references?: string[];
  showMemoryCard?: boolean;
  signedIn?: boolean;
  composerPlaceholder?: string;
  onPrompt?: (prompt: string) => void;
  onClear?: () => void;
  onInputChange?: (value: string) => void;
  onSubmit?: () => void;
  onSignIn?: () => void;
  onDismissMemory?: () => void;
};

export function SarathiChatPanel({
  messages,
  prompts,
  welcomeMessage,
  input = "",
  busy = false,
  error = "",
  references = [],
  showMemoryCard = false,
  signedIn = false,
  composerPlaceholder = "Share what rests upon your heart...",
  onPrompt,
  onClear,
  onInputChange,
  onSubmit,
  onSignIn,
  onDismissMemory,
}: SarathiChatPanelProps) {
  return (
    <>
      <main className="krishna-reference-grid production-krishna-grid">
        <section className="krishna-chat-shell">
          <div className="krishna-chat-hero">
            <div>
              <p>Let Sri Krishna Guide you</p>
              <h1>What rests upon your heart?</h1>
              <div className="krishna-lotus-rule" aria-hidden="true">
                <span />
                <img src="/brand/splash-lotus-icon.png" alt="" />
                <span />
              </div>
              <p className="krishna-hero-copy">Step into a quiet space of devotion, and rest near Krishna's wisdom.</p>
            </div>
            {onClear && (
              <button type="button" className="krishna-clear-button" onClick={onClear}>
                <Trash2 size={18} />
                Clear
              </button>
            )}
          </div>

          {messages.length === 0 && (
            <div className="krishna-prompt-row" aria-label="Suggested first messages">
              {prompts.map((prompt) => (
                <button key={prompt} type="button" disabled={busy} onClick={() => onPrompt?.(prompt)}>
                  {prompt}
                </button>
              ))}
            </div>
          )}

          <section className="krishna-thread" aria-live="polite">
            {messages.length === 0 && (
              <SarathiAssistantMessage text={welcomeMessage} />
            )}
            {messages.map((message) => (
              message.role === "assistant" ? (
                <SarathiAssistantMessage key={message.id} text={message.text} />
              ) : (
                <div className="krishna-user-row" key={message.id}>
                  <article className="krishna-user-card">{message.text}</article>
                </div>
              )
            ))}
            {busy && <SarathiAssistantMessage text="The charioteer is reflecting..." />}
          </section>

          <div className="krishna-trust-row">
            <span><ShieldCheck size={18} /> Confidential</span>
            <i aria-hidden="true" />
            <span><img src="/brand/splash-lotus-icon.png" alt="" /> Rooted in Dharma</span>
            <i aria-hidden="true" />
            <span><span className="krishna-heart" aria-hidden="true">&#9825;</span> Always Compassionate</span>
          </div>

          {error && <p className="krishna-chat-error">{error}</p>}

          {showMemoryCard && (
            <aside className="sarathi-memory-card krishna-memory-reminder">
              <div>
                <strong>Sign in to make Sarathi remember what matters to you.</strong>
                <p>Your reflections, your journey - always held with care.</p>
              </div>
              <div className="sarathi-memory-actions">
                <button type="button" onClick={onSignIn}>Sign in</button>
                {onDismissMemory && (
                  <button type="button" aria-label="Dismiss sign-in reminder" onClick={onDismissMemory}>x</button>
                )}
              </div>
            </aside>
          )}

          {references.length > 0 && (
            <details className="sarathi-reference-row krishna-reference-row">
              <summary>Verse references</summary>
              <ul>
                {references.map((source) => <li key={source}>{source}</li>)}
              </ul>
            </details>
          )}

          <div className="krishna-composer">
            <label className="sr-only" htmlFor="sarathi-chat-input">Message for Sarathi</label>
            <textarea
              id="sarathi-chat-input"
              aria-label="Message for Sarathi"
              value={input}
              onChange={(event) => onInputChange?.(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter" && !event.shiftKey) {
                  event.preventDefault();
                  onSubmit?.();
                }
              }}
              placeholder={composerPlaceholder}
            />
            <button type="button" aria-label="Send message" disabled={busy || !input.trim()} onClick={onSubmit}>
              <Send size={24} />
            </button>
          </div>
        </section>

        <SarathiSidebar signedIn={signedIn} onSignIn={onSignIn} />
      </main>

      <footer className="krishna-reference-footer">
        Sarathi is an AI companion inspired by timeless wisdom.<br />
        Not a substitute for professional advice.
      </footer>
    </>
  );
}

function SarathiAssistantMessage({ text }: { text: string }) {
  return (
    <div className="krishna-assistant-group">
      <div className="krishna-speaker-row">
        <span className="krishna-avatar">
          <img src="/brand/sarathi-logo.png" alt="" />
        </span>
        <strong>Sarathi</strong>
      </div>
      <article className="krishna-assistant-card">{text}</article>
    </div>
  );
}

function SarathiSidebar({ signedIn, onSignIn }: { signedIn: boolean; onSignIn?: () => void }) {
  return (
    <aside className="krishna-sidebar">
      <article className="krishna-side-panel reflection">
        <div className="krishna-panel-heading">
          <Sun size={28} />
          <p>Today's Reflection</p>
        </div>
        <div className="krishna-quote-card">
          <span aria-hidden="true">"</span>
          <blockquote>
            Meet this day with a steady heart. Offer the work, release the weight,
            and let devotion make the next step gentle.
          </blockquote>
          <cite>Daily guidance</cite>
        </div>
      </article>

      <article className="krishna-side-panel verse">
        <div className="krishna-panel-heading">
          <BookOpen size={27} />
          <p>Verse of the Day</p>
        </div>
        <div className="krishna-verse-card">
          <p className="sanskrit">
            karmany evadhikaras te ma phalesu kadacana<br />
            ma karma-phala-hetur bhur ma te sango 'stv akarmani
          </p>
          <strong>Bhagavad Gita 2.47</strong>
          <p>
            You have the right to your action alone, never to its fruits.
            Let not the fruits of action be your motive, nor let your
            attachment be to inaction.
          </p>
          <a href="/mock/sarathi-design-2">Explore this verse <ArrowRight size={18} /></a>
        </div>
      </article>

      {signedIn ? <SacredMemoryCard /> : <MakeSarathiYoursCard onSignIn={onSignIn} />}

      <article className="krishna-side-panel privacy">
        <ShieldCheck size={34} />
        <p><strong>Your privacy is sacred.</strong><br />Conversations are private and secure.</p>
      </article>
    </aside>
  );
}

function MakeSarathiYoursCard({ onSignIn }: { onSignIn?: () => void }) {
  return (
    <article className="krishna-side-panel signin">
      <div className="krishna-panel-heading">
        <User size={26} />
        <p>Make Sarathi Yours</p>
      </div>
      <p>Sign in to save your conversations, continue where you left off, and help Sarathi remember what matters to you.</p>
      <ul>
        <li><Check size={16} /> Your reflections, securely saved</li>
        <li><Check size={16} /> Personalized guidance over time</li>
        <li><Check size={16} /> A companion that grows with you</li>
      </ul>
      <button type="button" onClick={onSignIn}>Sign in</button>
    </article>
  );
}

function SacredMemoryCard() {
  return (
    <article className="krishna-side-panel sacred-memory">
      <div className="krishna-panel-heading">
        <Sparkles size={26} />
        <p>Your Sacred Memory</p>
      </div>
      <p>Rest your heart at Krishna's feet.</p>
      <Link to="/settings" className="krishna-memory-button">Clarify Your Soul</Link>
    </article>
  );
}
