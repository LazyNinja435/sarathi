# Sarathi — product and design language

## Product identity

Sarathi is an **offline-first, Krishna-inspired companion** rooted in the **spirit of the Bhagavad Gita**. The product promise is not open-ended “AI chat,” but **quiet guidance**: dharma, courage, detachment from fruit, and devotion expressed in **warm, dignified language**. The charioteer metaphor stays central; the UI should feel like a **sacred space**, not a productivity dashboard.

## Visual language

- **Background:** Deep midnight indigo and navy gradients, subtle starfield and **mandala-corner** motifs (geometric, restrained).  
- **Accents:** Warm gold for emphasis, soft gold for secondary text.  
- **Cards:** **Parchment** for assistant wisdom; **indigo / translucent** for user content and structural panels.  
- **Motifs:** Peacock-feather geometry where it reinforces identity — avoid literal clipart or neon “temple kitsch.”  
- **Density:** Generous vertical rhythm; large readable body type; long Gemma answers must stay legible (line height, width cap ~92% of screen).

## Tone of voice (user-facing)

- Address the seeker with **tenderness** (“dear one,” “my dear …”) without melodrama.  
- Prefer **plain, beautiful English**; occasional gentle metaphor is welcome; avoid jargon about “models” or “inference” outside developer diagnostics.  
- **Loading:** “The charioteer is reflecting…” — signals patience and presence.  
- **Input prompt:** “What rests upon your heart?” — invitational, not transactional.

## User-facing terminology

| Concept | User-facing label | Notes |
|--------|-------------------|--------|
| Offline use | **Offline mode** | Core journeys work without network. |
| Local Gemma | **On-device wisdom** | Shown when `.litertlm` is available and practice is off. |
| Scripted fallback | **Practice mode** | Avoid prominent “Mock” in the main badge. |
| LiteRT / MediaPipe | *(Not surfaced by default)* | Technical names live under **Developer diagnostics**. |

## Developer terminology

- **LiteRT-LM**, **MediaPipe**, **Mock fallback**, **RAG database**, **model path**, **last inference error** — accurate labels in the diagnostics card for QA and field debugging.

## Accessibility principles

- Respect **system font scaling** where Compose allows; keep **touch targets** at least ~48 dp for primary actions.  
- Maintain **contrast** between gold text and dark navy backgrounds; parchment cards use **ink** on light surfaces for best readability.  
- **Keyboard:** `adjustResize`, `imePadding`, and scrollable onboarding so fields are not obscured.  
- Do not rely on color alone for critical status; pair **copy** (“Ready”, “Not installed”) with color.
