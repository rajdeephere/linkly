# Linkly — Day-by-Day Build Roadmap

> Companion to **[ARCHITECTURE.md](./ARCHITECTURE.md)**. A day-by-day plan to go from empty repo → a deployed, fast, analytics-rich link platform with branded domains and at least one flagship feature.

**Stack:** Spring Boot (api + resolver) · Next.js + Tailwind + shadcn/ui · Postgres · Redis · ClickHouse · Kafka · Edge (Cloudflare Workers / Vercel Edge) · AWS
**Last updated:** 2026-07-13

---

## How to read this

- **15 "days" = 15 focused work blocks**, not calendar days. Evenings/weekends → treat each as a milestone.
- The plan **front-loads correctness** (Days 1–5), then **analytics** (6–7), then **the read hot path at scale** (8–9), then **platform features** (10–13), then **ship it** (14–15). This mirrors the golden rule: *correct and safe on one server before fast across the globe.*
- Each day has a **single demoable outcome** — if you can't demo it, the day isn't done.
- **Cut line:** Days 1–9 are the must-ship core (a fast, correct, analytics-rich, scaled shortener). Days 10–15 are where you choose depth.

---

## Week 1 — A correct, safe shortener on one service

### Day 1 — Scaffold + local infra + domain skeleton
- Monorepo: `apps/api` (Spring Boot: web, validation, JPA, Postgres, Redis), `apps/web` (Next.js + Tailwind + shadcn).
- `docker compose -f infra/docker-compose.yml up -d` (Postgres, Redis, ClickHouse, Kafka).
- `Workspace`, `User`, `Link` entities + Flyway/Liquibase migration.
- **Demo:** app boots, health endpoint green, one link row seeded via migration.
- *Maps to:* ARCH §10 data model.

### Day 2 — Shorten + resolve (the core loop)
- `POST /v1/links` (destination → code) and `GET /{code}` → **302** redirect.
- `Link` unique constraint on `(domain_id, code)`.
- Next.js: a shorten bar with **one-click copy** (the "aha" moment, <1s).
- **Demo:** paste a URL → get a short link → click it → land on the destination.
- *Maps to:* ADR-5, ARCH §3–4.

### Day 3 — Non-enumerable code generation (KGS) ⭐
- Key Generation Service: pre-allocated **base62** pool in Redis, non-sequential (shuffled counter); atomic claim on create; low-watermark refill.
- Prove codes are **not** walkable (`a, b, c…`).
- **Demo:** create 1000 links; show codes are unique, non-sequential; run an enumeration script that fails to scrape.
- *Maps to:* ADR-2, ARCH §9, scenario #6/#7.

### Day 4 — Custom aliases, expiry, safety
- Custom alias (clean `409` on clash); expiry by date + click cap → `410 Gone` / `expiresUrl`.
- **Safe-Browsing** check on create → reject unsafe URLs; per-IP rate limit.
- Next.js: create modal (alias, expiry, live preview).
- **Demo:** claim a vanity slug; an expired link returns 410; a known-bad URL is rejected.
- *Maps to:* ADR-9, scenarios #2, #5, #9.

### Day 5 — Edit / delete + cache-aside + invalidation
- Redis cache-aside on resolve (`link:{host}:{code}`); `PATCH`/`DELETE` **write-through + purge**.
- **Demo:** edit a live link's destination → next click goes to the **new** URL immediately (no stale redirect); delete → 404.
- *Maps to:* ADR-8, scenario #3.

> **End of Week 1 = a correct, safe, editable shortener with non-enumerable codes and a clean dashboard, on one service.** Bedrock. Don't proceed until an edit is reflected instantly and codes can't be scraped.

---

## Week 2 — Analytics + the read hot path at scale

### Day 6 — Click event pipeline (off the hot path) ⭐
- Resolver emits a **fire-and-forget** click event to Kafka **after** the redirect.
- Consumer enriches (geo / device / browser / bot flag), **hashes IP** (GDPR), batch-inserts into **ClickHouse**.
- **Demo:** click a link 100× → events land in ClickHouse; kill the consumer → redirects **keep working**.
- *Maps to:* ADR-4, ARCH §8, scenario #4, #14.

### Day 7 — Analytics dashboard
- Aggregation queries (timeseries, byCountry, byDevice, byReferrer, top links); API endpoints.
- Next.js: analytics view — time-series chart, **geo map**, device/referrer breakdowns, date-range picker (Tremor/Recharts).
- **Demo:** a genuinely good-looking analytics page for a link.
- *Maps to:* ADR-7, ARCH §8.

### Day 8 — Split the resolver + edge KV (go global) ⭐
- Extract `apps/resolver` from `apps/api` (independent deploy/scale — ADR-1).
- Stand up the **edge resolver** (Cloudflare Worker / Vercel Edge) with **edge KV**; origin **warms** the edge on miss; edits **purge** it.
- **Demo:** a hot link resolves from the edge in <10ms; an edit purges and re-warms correctly.
- *Maps to:* ADR-1, ADR-3, scenario #1, #11. **Signature war-story.**

### Day 9 — Load test + resiliency
- k6/Gatling: hammer a viral link; measure p99 resolve latency; confirm origin/Postgres stay cool behind the edge.
- Graceful degradation: origin down → edge serves last-known (stale-while-revalidate).
- **Demo:** 100k req/s load test with <10ms p99; origin-down drill with no user-facing 5xx.
- *Maps to:* scenarios #1, #11. **Second war-story.**

> **End of Day 9 = a fast, correct, analytics-rich, horizontally-scaled shortener.** This alone is a strong portfolio project. Everything after is *platform depth*.

---

## Week 3 — Platform features + ship it

### Day 10 — Branded custom domains + TLS
- Add-domain flow → DNS **TXT/CNAME verification** → on-demand per-host TLS → resolver routes by `Host`.
- **Demo:** point `go.acme.com` at Linkly → serves branded short links over HTTPS.
- *Maps to:* ADR-6, scenario #8.

### Day 11 — QR + smart routing
- Dynamic **QR codes** per link (stay valid on edit) → S3.
- **Smart routing**: geo / device / OS rules + **A-B split** with sticky bucketing (deep-link to app stores).
- **Demo:** one link sends iOS→App Store, Android→Play, else web; a QR that survives a destination edit.
- *Maps to:* ADR-10, scenario #12, #13, #15.

### Day 12 — Teams, API keys, bulk
- Workspaces + **RBAC**; scoped **API keys**; public API; **bulk CSV import** (async job, batch KGS allocation).
- **Demo:** invite a teammate with a role; create links via API key; import 10k links.
- *Maps to:* ARCH §10, scenario #16.

### Day 13 — Flagship (PICK ONE) ⭐
Do one **well**, not three half-built:
- **Link-in-bio** (recommended, best product ROI): hosted mini-page builder + themes, served at the edge.
- **Password-protected + cloaked links** + interstitial warning pages for flagged links.
- **Deep-link intelligence**: universal links / app-clip handling, richer device targeting.
- **Demo:** the flagship working end-to-end.
- *Maps to:* ARCH §12, spec §10 Phase 4.

### Day 14 — Deploy: cloud + CI/CD + observability
- Dockerize api/resolver; deploy to ECS/EKS behind an ALB; edge on Cloudflare/Vercel; web on Vercel.
- GitHub Actions: build → test → push → deploy. Terraform for infra.
- **OpenTelemetry** tracing a resolve end-to-end (edge → miss → origin → Redis → Postgres → event); Grafana dashboard.
- **Demo:** live public URL; a trace of one resolve crossing edge → origin.

### Day 15 — Harden, document, war-stories
- Rate limits, abuse guard, GDPR retention config; security pass (open-redirect checks, auth).
- Write the **README polish + the war-stories** (edge/origin hybrid + load test; non-enumerable codes vs enumeration attack; cache-invalidation drill) as STAR stories for interviews.
- **Demo:** a documented, deployed, load-tested platform with a clear "here's what's hard and how I solved it" writeup.

---

## Milestone summary

| Day | Milestone | Demoable proof |
|---|---|---|
| 1–2 | Shorten + resolve | Paste → short link → redirect |
| **3** | **Non-enumerable codes** ⭐ | **Enumeration script fails to scrape** |
| 4 | Aliases / expiry / safety | Vanity slug; 410; bad URL rejected |
| 5 | Edit + invalidation | Edit → new URL served instantly |
| **6** | **Analytics off hot path** ⭐ | **Consumer down → redirects still work** |
| 7 | Analytics dashboard | Map + time-series + breakdowns |
| **8** | **Edge + split resolver** ⭐ | **Hot link <10ms from the edge** |
| **9** | **Load test + resiliency** ⭐ | **100k req/s, p99 <10ms; origin-down OK** |
| 10 | Custom domains + TLS | `go.acme.com` over HTTPS |
| 11 | QR + smart routing | iOS/Android/web split; durable QR |
| 12 | Teams / API / bulk | Roles; API key; 10k import |
| **13** | **Flagship** ⭐ | **Link-in-bio / cloaking / deep links** |
| 14 | Deployed + observable | Public URL + resolve trace |
| 15 | Hardened + documented | Load test + war-stories |

---

## Adjusting the plan

- **Behind schedule?** Cut Days 11–13; ship a deployed Days-1–10 system. A *deployed, fast, analytics-rich* shortener with branded domains beats a half-built fancy feature.
- **Ahead / want depth?** Stack a second Day-13 flagship, or push toward multi-region edge + billing/plans (spec §10 Phase 5).
- **Interview-prep mode?** After Day 15, write one ADR-style doc per [ARCHITECTURE.md §12](./ARCHITECTURE.md#12-architecture-decision-records-adrs) decision and one STAR story per hard scenario you actually triggered — that's the interview gold the build exists to produce.

> **The non-negotiable spine:** Days 1–5 (correct + safe) → Days 6 & 8–9 (analytics off the path + scaled + resilient) → Day 14 (deployed). Everything else flexes around that.
