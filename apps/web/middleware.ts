import { Redis } from "@upstash/redis";
import { NextFetchEvent, NextRequest, NextResponse } from "next/server";

// Edge KV over the Upstash HTTP protocol (what "Vercel KV" is now). Locally these point at the SRH
// proxy over linkly-redis (infra/docker-compose.yml); on Vercel they're the KV integration's env vars.
const kv = new Redis({
  url: process.env.KV_REST_API_URL ?? "http://localhost:8079",
  token: process.env.KV_REST_API_TOKEN ?? "devtoken",
});

// The origin resolver (apps/resolver). Called only on a cache miss.
const RESOLVER = process.env.RESOLVER_URL ?? "http://localhost:8082";

const CODE = /^\/([0-9A-Za-z]{1,64})$/;
const EDGE_TTL = 3600; // seconds — a backstop; edits purge the key explicitly (ADR-0008)

/**
 * Edge resolver (ADR-0003). Fronts `/{code}`:
 *  - always fire-and-forget the click to the resolver's /ingest (analytics survives a cache hit)
 *  - KV hit  → 302 from the edge, no origin call
 *  - KV miss → ask the resolver /r/{code}; cache if cacheable; 302 / 410 / 404
 */
export async function middleware(req: NextRequest, event: NextFetchEvent) {
  const match = req.nextUrl.pathname.match(CODE);
  if (!match) return NextResponse.next();
  const code = match[1];
  // The host the request came in on — selects the domain (uniqueness is per (domain, code)).
  const host = req.headers.get("host") ?? "";

  const click = {
    code,
    ip: req.headers.get("x-forwarded-for") ?? "",
    userAgent: req.headers.get("user-agent") ?? "",
    referer: req.headers.get("referer") ?? "",
    country: req.headers.get("x-vercel-ip-country") ?? req.geo?.country ?? null,
    ts: Date.now(),
  };
  event.waitUntil(
    fetch(`${RESOLVER}/ingest/click`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(click),
    }).catch(() => {}),
  );

  const key = `edge:link:${host}:${code}`;

  const cached = await kv.get<string>(key).catch(() => null);
  if (cached) {
    return NextResponse.redirect(cached, 302);
  }

  // Forward the routing signals so the resolver can evaluate device/OS/geo/A-B rules (ADR-0010).
  const params = new URLSearchParams({
    host,
    ua: click.userAgent,
    country: click.country ?? "",
    ip: click.ip,
  });
  const res = await fetch(`${RESOLVER}/r/${code}?${params}`).catch(() => null);
  if (!res || !res.ok) return new NextResponse(null, { status: 404 });
  const outcome = (await res.json()) as { status: string; url: string; cacheable: boolean };

  if (outcome.status === "REDIRECT") {
    if (outcome.cacheable) {
      event.waitUntil(kv.set(key, outcome.url, { ex: EDGE_TTL }).then(() => {}).catch(() => {}));
    }
    return NextResponse.redirect(outcome.url, 302);
  }
  if (outcome.status === "GONE") return new NextResponse(null, { status: 410 });
  return new NextResponse(null, { status: 404 });
}

// Run on everything except Next internals, the API proxy, and static assets; the code-shape check
// inside the handler decides what to actually intercept.
export const config = {
  matcher: "/((?!_next/|api/|favicon.ico).*)",
};
