import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname } from "node:path";

export interface GuestStats {
  totalGuestMessages: number;
}

export interface GuestUsageStore {
  incrementGuestMessages(): Promise<void>;
  readGuestStats(): Promise<GuestStats>;
}

export function createFileGuestUsageStore(filePath: string): GuestUsageStore {
  return {
    async incrementGuestMessages() {
      const current = await readStats(filePath);
      await writeStats(filePath, { totalGuestMessages: current.totalGuestMessages + 1 });
    },
    async readGuestStats() {
      return readStats(filePath);
    }
  };
}

async function readStats(filePath: string): Promise<GuestStats> {
  try {
    const raw = await readFile(filePath, "utf8");
    const parsed = JSON.parse(raw) as Partial<GuestStats>;
    return { totalGuestMessages: Math.max(0, Number(parsed.totalGuestMessages ?? 0)) };
  } catch {
    return { totalGuestMessages: 0 };
  }
}

async function writeStats(filePath: string, stats: GuestStats) {
  await mkdir(dirname(filePath), { recursive: true });
  await writeFile(filePath, JSON.stringify(stats, null, 2), "utf8");
}
