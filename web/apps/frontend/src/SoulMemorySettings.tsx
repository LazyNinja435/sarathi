import { Check, Edit3, Plus, RotateCcw, Save, Trash2 } from "lucide-react";
import { useState } from "react";

export type SoulMemorySectionKey =
  | "journey"
  | "recurringQuestions"
  | "helpfulGuidance"
  | "practices"
  | "reflections"
  | "lifeContext";

export type SoulMemoryItem = {
  id: string;
  text: string;
  tags?: string[];
  importance?: 1 | 2 | 3 | 4 | 5;
  createdAt?: string;
  updatedAt?: string;
  status?: "active" | "completed";
  source?: {
    type: "gita_verse" | "reflection" | "chat";
    reference?: string;
  };
};

export type SoulMemory = {
  schema: "sarathi.memory.v1";
  userId?: string;
  updatedAt?: string;
  memoryEnabled: boolean;
  sections: Record<SoulMemorySectionKey, SoulMemoryItem[]>;
};

type SoulSectionConfig = {
  key: SoulMemorySectionKey;
  title: string;
  description: string;
  placeholder: string;
};

const sectionConfigs: SoulSectionConfig[] = [
  {
    key: "journey",
    title: "My Journey",
    description: "Things Sarathi should understand about where you are in life right now.",
    placeholder: "I am trying to stay calmer at work.",
  },
  {
    key: "recurringQuestions",
    title: "Questions I Return To",
    description: "Doubts, reflections, and themes you often bring to Krishna's wisdom.",
    placeholder: "How do I do my duty without attachment?",
  },
  {
    key: "helpfulGuidance",
    title: "Guidance That Helped Me",
    description: "Verses, reflections, or ideas you want Sarathi to remember.",
    placeholder: "Bhagavad Gita 2.47 helped me understand action without attachment.",
  },
  {
    key: "practices",
    title: "Practices I'm Trying",
    description: "Small spiritual or practical steps you are working on.",
    placeholder: "Pause and breathe before reacting.",
  },
  {
    key: "reflections",
    title: "Personal Reflections",
    description: "Private notes, moments of clarity, or thoughts you may want Sarathi to consider.",
    placeholder: "I felt peaceful after reading chapter 12.",
  },
  {
    key: "lifeContext",
    title: "Life Context",
    description: "A few practical details that help Sarathi guide you with care.",
    placeholder: "I work in a stressful job.",
  },
];

function createMockSoulMemory(): SoulMemory {
  return {
    schema: "sarathi.memory.v1",
    memoryEnabled: true,
    sections: {
      journey: [
        {
          id: "journey_001",
          text: "I am trying to stay calmer at work.",
        },
      ],
      recurringQuestions: [
        {
          id: "question_001",
          text: "How do I do my duty without attachment?",
        },
      ],
      helpfulGuidance: [
        {
          id: "guidance_001",
          text: "Bhagavad Gita 2.47 helped me understand action without attachment.",
          source: {
            type: "gita_verse",
            reference: "2.47",
          },
        },
      ],
      practices: [
        {
          id: "practice_001",
          text: "Pause and breathe before reacting.",
          status: "active",
        },
      ],
      reflections: [
        {
          id: "reflection_001",
          text: "I felt peaceful after reading chapter 12.",
        },
      ],
      lifeContext: [
        {
          id: "context_001",
          text: "I work in a stressful job.",
        },
      ],
    },
  };
}

export function SoulMemorySettings() {
  const [memory, setMemory] = useState<SoulMemory>(() => createMockSoulMemory());
  const [editingIds, setEditingIds] = useState<Set<string>>(() => new Set());
  const [savedMessage, setSavedMessage] = useState("");

  function updateSection(key: SoulMemorySectionKey, updater: (items: SoulMemoryItem[]) => SoulMemoryItem[]) {
    setSavedMessage("");
    setMemory((current) => ({
      ...current,
      updatedAt: new Date().toISOString(),
      sections: {
        ...current.sections,
        [key]: updater(current.sections[key]),
      },
    }));
  }

  function addNote(config: SoulSectionConfig) {
    const id = `${config.key}_${Date.now()}`;
    updateSection(config.key, (items) => [
      ...items,
      {
        id,
        text: "",
        status: config.key === "practices" ? "active" : undefined,
      },
    ]);
    setEditingIds((current) => new Set(current).add(id));
  }

  function updateText(key: SoulMemorySectionKey, id: string, text: string) {
    updateSection(key, (items) => items.map((item) => item.id === id ? { ...item, text } : item));
  }

  function removeNote(key: SoulMemorySectionKey, id: string) {
    updateSection(key, (items) => items.filter((item) => item.id !== id));
    setEditingIds((current) => {
      const next = new Set(current);
      next.delete(id);
      return next;
    });
  }

  function toggleEdit(id: string) {
    setEditingIds((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function markPracticeComplete(id: string) {
    updateSection("practices", (items) => items.map((item) => (
      item.id === id ? { ...item, status: "completed" } : item
    )));
  }

  function discardChanges() {
    setMemory(createMockSoulMemory());
    setEditingIds(new Set());
    setSavedMessage("");
  }

  function saveChanges() {
    console.log("Soul memory mock save", memory);
    setSavedMessage("Saved");
  }

  function clearAll() {
    if (!window.confirm("Clear all soul notes?")) return;
    setMemory((current) => ({
      ...current,
      updatedAt: new Date().toISOString(),
      sections: {
        journey: [],
        recurringQuestions: [],
        helpfulGuidance: [],
        practices: [],
        reflections: [],
        lifeContext: [],
      },
    }));
    setEditingIds(new Set());
    setSavedMessage("");
  }

  return (
    <section className="soul-settings">
      <div className="soul-settings-hero">
        <p className="eyebrow">Settings</p>
        <h2>Clarify Your Soul</h2>
        <p>A quiet place to keep what matters on your path. Sarathi uses this only to offer more meaningful guidance.</p>
      </div>

      <div className="soul-section-grid">
        {sectionConfigs.map((config) => (
          <article className="soul-section-card" key={config.key}>
            <div className="soul-section-heading">
              <div>
                <h3>{config.title}</h3>
                <p>{config.description}</p>
              </div>
              <button type="button" className="soul-add-button" onClick={() => addNote(config)}>
                <Plus size={16} />
                Add note
              </button>
            </div>

            <div className="soul-note-list">
              {memory.sections[config.key].length === 0 && (
                <p className="soul-empty-note">No notes yet.</p>
              )}
              {memory.sections[config.key].map((item) => {
                const isEditing = editingIds.has(item.id);
                return (
                  <div className="soul-note" key={item.id}>
                    {isEditing ? (
                      <textarea
                        aria-label={`${config.title} note`}
                        value={item.text}
                        placeholder={config.placeholder}
                        onChange={(event) => updateText(config.key, item.id, event.target.value)}
                      />
                    ) : (
                      <p>{item.text || config.placeholder}</p>
                    )}

                    {config.key === "helpfulGuidance" && item.source?.reference && (
                      <span className="soul-source">Source: Gita {item.source.reference}</span>
                    )}

                    {config.key === "practices" && (
                      <div className="soul-status-row">
                        <span>Status: {item.status === "completed" ? "Completed" : "Active"}</span>
                        {item.status !== "completed" && (
                          <button type="button" onClick={() => markPracticeComplete(item.id)}>
                            <Check size={15} />
                            Mark complete
                          </button>
                        )}
                      </div>
                    )}

                    <div className="soul-note-actions">
                      <button type="button" onClick={() => toggleEdit(item.id)}>
                        <Edit3 size={15} />
                        {isEditing ? "Done" : "Edit"}
                      </button>
                      <button type="button" className="danger-link" onClick={() => removeNote(config.key, item.id)}>
                        <Trash2 size={15} />
                        Remove
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </article>
        ))}
      </div>

      <footer className="soul-settings-footer">
        <p>You are always in control. Edit or remove anything Sarathi remembers.</p>
        <div className="soul-footer-actions">
          <button type="button" className="ghost" onClick={discardChanges}>
            <RotateCcw size={16} />
            Discard changes
          </button>
          <button type="button" className="primary-action small" onClick={saveChanges}>
            <Save size={16} />
            Save changes
          </button>
          <button type="button" className="ghost danger" onClick={clearAll}>
            <Trash2 size={16} />
            Clear all soul notes
          </button>
        </div>
        {savedMessage && <span className="soul-saved-message">{savedMessage}</span>}
      </footer>
    </section>
  );
}
