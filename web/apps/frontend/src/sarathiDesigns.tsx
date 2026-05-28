import {
  ArrowRight,
  BookOpen,
  Check,
  LogIn,
  MessageCircle,
  Send,
  Settings,
  ShieldCheck,
  Sparkles,
  Sun,
  Trash2,
  User,
} from "lucide-react";
import { Link, Navigate } from "react-router-dom";
import { getVerseOfTheDay } from "./gitaVerseOfDay";

export interface SarathiDesignMessage {
  id: string;
  role: string;
  text: string;
}

const mockPrompts = [
  "I feel anxious",
  "I need clarity",
  "I am attached to an outcome",
  "I failed at something",
  "I want to understand my dharma",
  "Teach me a verse",
];

const mockWelcome = "My dear one,\n\nI have been seated quietly in the chariot of your heart.\n\nTell me - what battle stands before you today?";

export function MockDesignIndex() {
  return <Navigate to="/mock/sarathi-design-2" replace />;
}

export function SarathiDesignOnePage() {
  return <Navigate to="/mock/sarathi-design-2" replace />;
}

export function SarathiDesignThreePage() {
  return <Navigate to="/mock/sarathi-design-2" replace />;
}

export function SarathiDesignTwoPage({ signedIn = false }: { signedIn?: boolean }) {
  return (
    <div className="krishna-reference-page">
      <header className="krishna-reference-nav">
        <Link to="/chat" className="krishna-reference-brand">
          <img src="/brand/sarathi-logo.png" alt="" />
          <span>Sarathi</span>
        </Link>
        <nav aria-label="Sarathi navigation">
          <Link to="/chat"><MessageCircle size={22} /> Chat</Link>
          <Link to="/settings"><Settings size={22} /> Settings</Link>
          <button type="button"><User size={22} /> Sign in</button>
          <button type="button"><LogIn size={22} /> Exit guest</button>
        </nav>
      </header>

      <main className="krishna-reference-grid">
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
            <button type="button" className="krishna-clear-button">
              <Trash2 size={18} />
              Clear
            </button>
          </div>

          <div className="krishna-prompt-row">
            {mockPrompts.map((prompt) => (
              <button type="button" key={prompt}>{prompt}</button>
            ))}
          </div>

          <section className="krishna-thread">
            <div className="krishna-speaker-row">
              <span className="krishna-avatar">
                <img src="/brand/sarathi-logo.png" alt="" />
              </span>
              <strong>Sarathi</strong>
            </div>
            <article className="krishna-assistant-card">{mockWelcome}</article>
            <time>9:41 AM</time>
          </section>

          <div className="krishna-trust-row">
            <span><ShieldCheck size={18} /> Confidential</span>
            <i aria-hidden="true" />
            <span><img src="/brand/splash-lotus-icon.png" alt="" /> Rooted in Dharma</span>
            <i aria-hidden="true" />
            <span><span className="krishna-heart" aria-hidden="true">&#9825;</span> Always Compassionate</span>
          </div>

          <div className="krishna-composer">
            <label className="sr-only" htmlFor="krishna-reference-input">Message for Sarathi</label>
            <textarea id="krishna-reference-input" aria-label="Message for Sarathi" placeholder="Share what rests upon your heart..." />
            <button type="button" aria-label="Send message"><Send size={24} /></button>
          </div>
        </section>

        <SarathiSidebar signedIn={signedIn} />
      </main>

      <footer className="krishna-reference-footer">
        Sarathi is an AI companion inspired by timeless wisdom.<br />
        Not a substitute for professional advice.
      </footer>
    </div>
  );
}

export function SarathiDesignTwoSignedInPage() {
  return <SarathiDesignTwoPage signedIn />;
}

export function SarathiSidebar({ signedIn = false }: { signedIn?: boolean }) {
  const verseOfTheDay = getVerseOfTheDay();
  const sanskritLines = verseOfTheDay.sanskrit.split(/\r?\n/).filter(Boolean);

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
            {sanskritLines.map((line, index) => (
              <span key={`${verseOfTheDay.id}-${index}`}>
                {line}
                {index < sanskritLines.length - 1 && <br />}
              </span>
            ))}
          </p>
          <strong>Bhagavad Gita {verseOfTheDay.reference}</strong>
          <p>{verseOfTheDay.translation}</p>
          <a href="/mock/sarathi-design-2">Explore this verse <ArrowRight size={18} /></a>
        </div>
      </article>

      {signedIn ? <SacredMemoryCard /> : <MakeSarathiYoursCard />}

      <article className="krishna-side-panel privacy">
        <ShieldCheck size={34} />
        <p><strong>Your privacy is sacred.</strong><br />Conversations are private and secure.</p>
      </article>
    </aside>
  );
}

function MakeSarathiYoursCard() {
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
      <button type="button">Sign in</button>
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
