# Linkly — Requirement Execution Plan

> The **what & why** of the build, phase by phase. Each phase is a verifiable milestone with a
> single demoable outcome. This is the altitude above the runbooks: it says *what to build and why
> now*; the [`../step-by-step-implementation/`](../step-by-step-implementation/) docs say *how*.

**Golden rule:** make resolve **correct and safe on one service** before you make it **fast across the
globe.** Most people invert this — wiring an edge cache before the link even persists safely — and drown
in cache-invalidation bugs. Phases 0–1 are the *real* project; the rest is depth.

## The arc

| Phase | Theme | Demoable outcome | Roadmap days | Status |
|-------|-------|------------------|--------------|--------|
| [00](./00-correct-safe-shortener.md) | Correct, safe shortener on one service | non-enumerable codes, edit reflected instantly, bad URLs rejected | 1–5 | 🔨 in progress (Days 1–2 ✅) |
| [01](./01-analytics-pipeline.md) | Analytics off the hot path | consumer down → redirects still work; map + time-series dashboard | 6–7 | ⬜ next |
| [02](./02-read-hot-path-at-scale.md) | Read hot path at scale ⭐ | hot link <10ms from the edge; 100k req/s load test | 8–9 | ⬜ |
| [03](./03-domains-qr-routing.md) | Branded domains + QR + smart routing | `go.acme.com` over HTTPS; geo/device split; durable QR | 10–11 | ⬜ |
| [04](./04-teams-api-flagship.md) | Teams/API/bulk + one flagship ⭐ | roles + API keys + bulk import; link-in-bio | 12–13 | ⬜ |
| [05](./05-deploy-and-productize.md) | Deploy + harden + productize | live public URL; resolve trace; load-tested + documented | 14–15 | ⬜ |

## How to read a phase doc
Each has: **Goal · Scope (in/out) · Architecture delta · Done-when checklist · Maps to (days/ADRs)**.
A phase isn't done until every "done-when" box is checked **and** you can demo it.

## Relationship to other docs
- **Why each decision** → [`../adr/`](../adr/)
- **How the app works** → [`../ARCHITECTURE.md`](../ARCHITECTURE.md)
- **Day-by-day** → [`../ROADMAP.md`](../ROADMAP.md)
- **Step-by-step build/deploy** → [`../step-by-step-implementation/`](../step-by-step-implementation/)
