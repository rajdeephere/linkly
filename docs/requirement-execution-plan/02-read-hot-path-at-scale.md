# Phase 02 — Read hot path at scale ⭐

**Status:** ✅ complete (Days 8–9) · **Roadmap:** Days 8–9

## Goal
Make resolve **fast globally and viral-proof**: split the resolver from the management API, put hot
links at the edge, and prove it under load and failure. **This is the headline / signature story.**

## Scope
**In:** extract `apps/resolver` from `apps/api` (independent scale); edge resolver (Cloudflare Worker /
Vercel Edge) + edge KV; origin warms edge on miss; edits purge edge; load test (k6/Gatling) at viral
throughput; graceful degradation when origin is down.
**Out:** custom domains, routing (Phase 3). No new product features — this phase is pure architecture.

## Architecture delta
```
   visitor ─► edge KV (hit → 302, emit event) ─miss─► Spring Boot origin ─► Redis ─► Postgres ─► warm KV
```
Resolver split ([ADR-0001](../adr/0001-resolver-separate-from-api.md)); hybrid edge/origin
([ADR-0003](../adr/0003-hybrid-edge-origin.md)); purge on edit ([ADR-0008](../adr/0008-cache-invalidation.md)).

## Done when
- [x] **Day 8:** resolver is its own deployable (`apps/resolver`); hot links served from **edge KV**
      (Vercel/Upstash via SRH locally) without touching origin; an edit **purges the edge** + re-warms.
      *(The <10ms latency target is measured under Day 9's load test.)*
- [x] **Day 9:** k6 load test through the edge (10,261 reqs, **0 failures, 100% 302**); **origin-down
      drill** — hot link still 302 from edge KV, cold link clean 404, management API unaffected.
      *(Absolute latency is `next dev`-bound locally; a real p99 needs a Vercel deploy — deferred.)*

**✅ Phase 2 complete** — read hot path scaled (edge KV) and resilient (survives an origin outage).

## Maps to
- ADRs: [0001 resolver split](../adr/0001-resolver-separate-from-api.md),
  [0003 hybrid edge](../adr/0003-hybrid-edge-origin.md), [0008 invalidation](../adr/0008-cache-invalidation.md)
- Hard scenarios: viral spike (#1), edit-reflected (#3), origin-down (#11)
