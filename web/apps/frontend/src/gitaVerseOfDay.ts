import { gitaVerseDayCycle, type GitaVerseRecord } from "./gitaVerseOfDayData";

const cycleStartUtc = Date.UTC(2026, 0, 1);
const millisecondsPerDay = 24 * 60 * 60 * 1000;

export type GitaVerseOfTheDay = GitaVerseRecord;

export function listGitaVersesForDayCycle(): GitaVerseOfTheDay[] {
  return gitaVerseDayCycle;
}

export function getVerseOfTheDay(date = new Date()): GitaVerseOfTheDay {
  const utcDay = Math.floor(
    Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()) / millisecondsPerDay,
  );
  const startDay = Math.floor(cycleStartUtc / millisecondsPerDay);
  const index = positiveModulo(utcDay - startDay, gitaVerseDayCycle.length);

  return gitaVerseDayCycle[index];
}

function positiveModulo(value: number, divisor: number) {
  return ((value % divisor) + divisor) % divisor;
}
