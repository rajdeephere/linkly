# Phase 02 — Read hot path at scale ⭐

**Status:** ⬜ · **Roadmap:** Days 8–9

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
- [ ] **Day 8:** resolver is its own deployable; a hot link resolves from the edge in **<10ms**; an
      edit purges + re-warms correctly.
- [ ] **Day 9:** load test sustains viral req/s at **p99 <10ms** with origin/Postgres cool behind the
      edge; origin-down drill degrades gracefully (no user-facing 5xx). Documented as a war-story.

## Maps to
- ADRs: [0001 resolver split](../adr/0001-resolver-separate-from-api.md),
  [0003 hybrid edge](../adr/0003-hybrid-edge-origin.md), [0008 invalidation](../adr/0008-cache-invalidation.md)
- Hard scenarios: viral spike (#1), edit-reflected (#3), origin-down (#11)
