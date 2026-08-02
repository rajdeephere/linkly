# Impl 08 — Day 8: Resolver split + edge (the read hot path at scale)

**Outcome:** the redirect hot path is now **edge (Vercel) → origin resolver (Spring Boot) → DB**, split
out of the management API (ADR-0001, ADR-0003). Hot links serve from edge **KV**; misses fall to the
resolver; edits **purge the edge**; clicks still flow to analytics. **Status:** ✅ shipped & verified.
*(Starts Phase 2; Day 9 is the load test.)*

## Two halves
**A. Origin resolver** — `apps/resolver`, a separate Spring Boot service owning resolve → 302, Redis
cache-aside, expiry/click-cap, and fire-and-forget click emission. Exposes `GET /r/{code}` (JSON
outcome) + `POST /ingest/click` for the edge. Shares Postgres/Redis/Kafka with the api. *(Domain classes
are a lean copy of the api's; a shared `linkly-core` module is the clean refactor, deferred.)*

**B. Edge** — `apps/web/middleware.ts` (Next.js Edge Middleware) fronts `/{code}`:
```
click → waitUntil(POST resolver /ingest/click)      # always, so analytics survives a cache hit
KV hit  → 302 from the edge (no origin call)
KV miss → GET resolver /r/{code} → if cacheable KV.set(edge:link:{code}, TTL) → 302 / 410 / 404
```

## Build order
1. **`apps/resolver`** — `ResolverApplication`, `Link`(read)+`LinkRepository`(findByCode,
   tryIncrementClick), `LinkCache`, `ResolveOutcome`(+`cacheable`), `ResolveService`, `RedirectController`
   (`/{code}`, standalone), `ResolveApiController` (`/r/{code}` + `/ingest/click`), `ClickEventMessage`/
   `Publisher` (Kafka, `add.type.headers:false` so the api consumer maps to its own type). Port 8082,
   `flyway.enabled=false`, `ddl-auto=validate` (api owns the schema).
2. **Edge KV sim** — compose `srh` service (`hiett/serverless-redis-http`) over `linkly-redis` → the
   Upstash HTTP protocol on `:8079`, so **`@upstash/redis`** works with no Vercel account. *(Used
   `@upstash/redis`, not the deprecated `@vercel/kv` — "Vercel KV" is Upstash under the hood now.)*
3. **`middleware.ts`** — the flow above; `@upstash/redis` client, `event.waitUntil` for the fire-and-forget
   ingest, matcher excludes `_next`/`api`/`favicon`, a base62 regex decides what to intercept.
4. **api** — `LinkCache.evict` now also deletes `edge:link:{code}` (**purge fan-out to the edge**,
   ADR-0008); `LINKLY_BASE_URL` → `:3000` (the edge is the public entry); **retired** the api's
   `RedirectController` + `ClickEventPublisher` + `AsyncConfig` (the resolver owns redirects/emission).
5. env — `apps/web/.env.local` (`KV_REST_API_URL`/`TOKEN`, `RESOLVER_URL`).

## Verify
```bash
# create → shortUrl is http://localhost:3000/{code}
curl -sI http://localhost:3000/$CODE                     # edge MISS → 302 (+ caches edge:link:{code})
docker exec linkly-redis redis-cli get edge:link:$CODE    # → the destination
curl -sI http://localhost:3000/$CODE                     # edge HIT → 302 from KV
curl -s -X PATCH :8081/v1/links/$ID -d '{"destinationUrl":"…/NEW"}'   # api purges origin + edge KV
docker exec linkly-redis redis-cli get edge:link:$CODE    # → (empty)
curl -sI http://localhost:3000/$CODE                     # → NEW (not stale), re-caches
```
Verified: miss→302+cache, hit-from-KV, edit purges edge KV, re-resolve is fresh, and clicks flowed
edge→ingest→resolver→Kafka→api→Postgres (+3 rows).

## Why (one line each)
Separate resolver → the hot path scales independently of the dashboard (ADR-0001). Edge KV in front →
hot links never touch origin (ADR-0003). Edge posts every click to `/ingest` → analytics survives a
cache hit (the resolver isn't called on hits). One-key purge fan-out (`edge:link:{code}`) → an edit is
correct at the edge, not TTL-eventually (ADR-0008). `@upstash/redis` over HTTP → works in the edge
runtime (no TCP) and is the non-deprecated KV client.

## Trade-offs / caveats
- **Local sim ≠ prod:** edge KV is the *same* Redis via SRH here; on Vercel it's a separate global store,
  and the purge fan-out becomes a KV-provider API call (eventually consistent — the ADR-0008 story).
- **Duplication:** the resolver copies ~6 small classes from the api; extract `linkly-core` to remove it.
- Day 9 proves the *scale* half: load test (p99) + origin-down graceful degradation.

## Decisions referenced
- [ADR-0001 resolver split](../../adr/0001-resolver-separate-from-api.md) ·
  [ADR-0003 hybrid edge/origin](../../adr/0003-hybrid-edge-origin.md) ·
  [ADR-0008 cache invalidation](../../adr/0008-cache-invalidation.md)
