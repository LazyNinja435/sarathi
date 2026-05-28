import { describe, expect, it } from "vitest";
import { getVerseOfTheDay, listGitaVersesForDayCycle } from "./gitaVerseOfDay";

describe("Gita verse of the day", () => {
  it("loads all 700 Gita verses for the daily cycle", () => {
    const verses = listGitaVersesForDayCycle();

    expect(verses).toHaveLength(700);
    expect(verses[0].reference).toBe("1.1");
    expect(verses[0].sanskrit).toContain("धृतराष्ट्र उवाच");
    expect(verses[699].reference).toBe("18.78");
  });

  it("moves one verse per UTC day and cycles back after 700 days", () => {
    const firstDay = new Date("2026-01-01T12:00:00Z");
    const secondDay = new Date("2026-01-02T12:00:00Z");
    const cycleDay = new Date("2027-12-02T12:00:00Z");

    expect(getVerseOfTheDay(firstDay).reference).toBe("1.1");
    expect(getVerseOfTheDay(secondDay).reference).toBe("1.2");
    expect(getVerseOfTheDay(cycleDay).reference).toBe("1.1");
  });

  it("keeps the Sanskrit verse as the top display text", () => {
    const verse = getVerseOfTheDay(new Date("2026-04-04T12:00:00Z"));

    expect(verse.reference).toBe("2.47");
    expect(verse.sanskrit).toContain("कर्मण्येवाधिकारस्ते मा फलेषु कदाचन");
    expect(verse.translation).toContain("Thy business is with the action only");
  });
});
