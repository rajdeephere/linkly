# Phase 00 — Correct, safe shortener on one service

**Status:** 🔨 in progress (Day 1 ✅ done) · **Roadmap:** Days 1–5

## Goal
A **correct, safe, editable** shortener running on a **single** service: shorten → resolve (302),
non-enumerable codes, custom aliases, expiry, and abuse defense. Ship nothing until a code **can't
collide, can't be enumerated**, and an **edit is reflected immediately**. This is the bedrock.

## Scope
**In:** monorepo scaffold (`apps/api` Spring Boot + `apps/web` Next.js); `Workspace`/`User`/`Link` +
Flyway; `POST /v1/links` + `GET /{code}` → 302; KGS base62 codes; custom aliases (409 on clash);
expiry (date + click cap → 410); edit/delete + Redis cache-aside + invalidation; Safe-Browsing scan +
rate limit; shorten bar with one-click copy.
**Out (later phases):** analytics pipeline, edge resolve, custom domains, smart routing, teams, deploy.
Single service only — no edge, no separate resolver yet.

## Architecture delta
```
   Next.js web ──REST──► single Spring Boot api ──► Postgres (+ Redis cache-aside on resolve)
   api: KGS code → persist → 302 resolve; edit → write-through + purge cache
```
Flyway-owned schema ([ADR-0012](../adr/0012-flyway-schema.md)); 302 not 301
([ADR-0005](../adr/0005-302-not-301.md)); non-enumerable codes ([ADR-0002](../adr/0002-kgs-base62-codes.md)).

## Done when
- [x] **Day 1:** monorepo scaffolded; `Workspace`/`User`/`Link` + Flyway V1; app boots green; `/ping`
      returns `seededLinks: 1`; `/actuator/health` UP; web builds.
- [ ] **Day 2:** `POST /v1/links` → code; `GET /{code}` → 302 to destination; shorten bar + copy.
- [ ] **Day 3:** KGS base62 codes — unique, non-sequential; an enumeration script fails to scrape.
- [ ] **Day 4:** custom alias (409 on clash); expiry (date + click cap → 410); Safe-Browsing rejects a
      bad URL; per-IP rate limit.
- [ ] **Day 5:** edit destination → next click hits the new URL immediately; delete → 404; Redis
      cache-aside + purge.

## Maps to
- ADRs: [0002 KGS](../adr/0002-kgs-base62-codes.md), [0005 302](../adr/0005-302-not-301.md),
  [0008 cache invalidation](../adr/0008-cache-invalidation.md),
  [0009 Safe Browsing](../adr/0009-safe-browsing-abuse.md),
  [0011 Next.js](../adr/0011-nextjs-frontend.md), [0012 Flyway](../adr/0012-flyway-schema.md)
- Runbooks: [`../step-by-step-implementation/implementation/`](../step-by-step-implementation/implementation/)
- Hard scenarios to prove: alias race (#2), enumeration (#6), edit-reflected (#3), phishing rejected (#5)
