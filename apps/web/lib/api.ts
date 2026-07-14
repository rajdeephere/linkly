// Thin client for the Linkly management API.
// In dev, calls are proxied via next.config.mjs rewrites to http://localhost:8081.

const API_BASE = process.env.API_URL ?? "http://localhost:8081";

export type ApiStatus =
  | { ok: true; app: string; status: string; seededLinks: number }
  | { ok: false };

/** Hits GET /ping to confirm the API (and its DB connection) is alive. */
export async function getApiStatus(): Promise<ApiStatus> {
  try {
    const res = await fetch(`${API_BASE}/ping`, { cache: "no-store" });
    if (!res.ok) return { ok: false };
    const data = await res.json();
    return { ok: true, ...data };
  } catch {
    return { ok: false };
  }
}
