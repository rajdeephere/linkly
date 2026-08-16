// Client-side session + an authed fetch wrapper. The JWT lives in localStorage; every API call attaches
// it as a Bearer token, and a 401 clears the session and bounces to /login.

export type Session = {
  token: string;
  userId: string;
  workspaceId: string;
  role: string;
  email: string;
};

const KEY = "linkly_session";

export function saveSession(s: Session) {
  localStorage.setItem(KEY, JSON.stringify(s));
}

export function getSession(): Session | null {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem(KEY);
  return raw ? (JSON.parse(raw) as Session) : null;
}

export function clearSession() {
  localStorage.removeItem(KEY);
}

/** Fetch against the API (proxied at /api/*), attaching the Bearer token. 401 → logout + redirect. */
export async function api(path: string, opts: RequestInit = {}): Promise<Response> {
  const s = getSession();
  const headers: Record<string, string> = { ...(opts.headers as Record<string, string>) };
  if (opts.body && !headers["Content-Type"]) headers["Content-Type"] = "application/json";
  if (s) headers["Authorization"] = `Bearer ${s.token}`;

  const res = await fetch(`/api${path}`, { ...opts, headers });
  if (res.status === 401 && typeof window !== "undefined") {
    clearSession();
    window.location.href = "/login";
  }
  return res;
}
