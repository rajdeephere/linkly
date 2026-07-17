# Linkly — Step-by-Step Implementation

> The granular **how**. Where [`../requirement-execution-plan/`](../requirement-execution-plan/) says
> *what to build and why*, these are the hands-on runbooks: exact steps, commands, key files, and
> verification. Two perspectives, two tracks:

| Track | Perspective | What it answers |
|-------|-------------|-----------------|
| [`implementation/`](./implementation/) | **Build** | How do I *write* this feature? (code, entities, endpoints, tests) |
| [`deployment/`](./deployment/) | **Run / Deploy** | How do I *run* and ship it? (infra, docker, ports, later cloud/CI/CD) |

## Numbering
Runbooks are numbered to track the build (`NN-dayN-topic.md`). A runbook is written **fully when its
feature ships**, and outlined before — so completed days are detailed, future days are stubs that get
filled in as we go (no speculative guesswork).

## Status

**Implementation track**
- [x] [01 — Day 1: monorepo scaffold + domain + boots-green](./implementation/01-day1-scaffold.md) ✅ full
- [x] [02 — Day 2: shorten + 302 resolve + shorten bar](./implementation/02-day2-shorten-resolve.md) ✅ full
- [ ] 03 — Day 3: KGS non-enumerable codes — outlined
- [ ] 04 — Day 4: aliases, expiry, Safe-Browsing, rate limit — outlined
- [ ] 05 — Day 5: edit/delete + Redis cache-aside + invalidation — outlined
- [ ] 06+ — analytics, edge split, domains, routing, teams, flagship … — outlined per phase

**Deployment track**
- [x] [01 — Local docker dev (infra + api + web)](./deployment/01-local-docker-dev.md) ✅ full
- [ ] 02 — Dockerize api + resolver + web (images) — outlined
- [ ] 03 — Cloud (ECS/EKS) + edge (Cloudflare/Vercel) + CI/CD (GitHub Actions) — outlined
- [ ] 04 — Observability (OTel → Prometheus/Grafana) — outlined

## See also
- App design: [`../ARCHITECTURE.md`](../ARCHITECTURE.md), [`../adr/`](../adr/), [`../DEPLOYMENT-ARCHITECTURE.md`](../DEPLOYMENT-ARCHITECTURE.md)
- The phased plan: [`../requirement-execution-plan/`](../requirement-execution-plan/) · Day-by-day: [`../ROADMAP.md`](../ROADMAP.md)
