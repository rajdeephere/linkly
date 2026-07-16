<div align="center">

# 🔗 Linkly

### *Every link, everywhere, instantly.*

A **future-proof URL shortener & link-management platform** — the redirect engine and analytics pipeline behind a Bitly/Dub-style product.

![status](https://img.shields.io/badge/status-in%20development-yellow)
![backend](https://img.shields.io/badge/backend-Spring%20Boot-6DB33F?logo=springboot&logoColor=white)
![frontend](https://img.shields.io/badge/frontend-Next.js-000000?logo=nextdotjs&logoColor=white)
![edge](https://img.shields.io/badge/hot%20path-hybrid%20edge-F38020?logo=cloudflare&logoColor=white)
![license](https://img.shields.io/badge/license-MIT-blue)

</div>

---

## What is Linkly?

Linkly turns long URLs into short, branded, trackable links — and resolves them in **single-digit milliseconds worldwide**. It looks like a CRUD app, but the interesting engineering is underneath: a redirect hot path that scales independently of writes, collision-free non-enumerable code generation, click analytics that never block a redirect, and multi-tenant custom domains.

> **The product is the redirect engine + the data it generates — not the "shorten" form.**
> `shorten → encode → resolve → route → redirect → capture → aggregate`

## ✨ Features


**Core**
- Shorten any URL → short code, with optional **custom aliases**
- **Fast global redirects** (hybrid edge cache + origin), default `302` for editability + analytics
- **Expiration** by date or click count; **password-protected** & **cloaked** links

**Platform**
- 📊 **Click analytics** — over time, by country, device, browser, referrer (off the hot path)
- 🌐 **Branded custom domains** with automatic per-hostname TLS
- 🎯 **Smart routing** — geo / device / OS / time targeting + A-B split testing
- 📱 **QR codes** (dynamic — stay valid when the link is edited)
- 🔗 **Link-in-bio** hosted pages
- 👥 **Teams / workspaces** with RBAC; scoped **API keys** + bulk import
- 🛡️ **Abuse defense** — Safe-Browsing scan on create + rate limiting

## 🏗️ Architecture at a glance

```
Visitor ─▶ EDGE (KV, 99% hot)  ──hit──▶ 302 redirect  ──▶ click event (async)
                │ miss                                        │
                ▼                                             ▼
        Spring Boot ORIGIN ─▶ Redis ─▶ Postgres        Kafka ─▶ ClickHouse
        (source of truth, full rule eval)              (analytics pipeline)

        Spring Boot MANAGEMENT API  ◀──REST──  Next.js web (dashboard, bio, analytics)
```

The **resolve path is a separate service** from the management/CRUD API — reads outnumber writes 100:1+ and must never contend with the dashboard for resources.

📐 **Full diagrams (sequence, data model, deployment) + all architecture decisions → [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md)**

## 🧰 Tech stack

| Layer | Choice |
|---|---|
| **Management API** | Java + Spring Boot |
| **Redirect resolver** | Spring Boot origin + edge (Cloudflare Workers / Vercel Edge) + edge KV |
| **Metadata store** | PostgreSQL |
| **Analytics store** | ClickHouse |
| **Cache / KGS pool** | Redis |
| **Event stream** | Kafka |
| **Web** | Next.js (App Router) + React + Tailwind + shadcn/ui |
| **Infra** | AWS + Terraform + Docker; GitHub Actions CI/CD |
| **Observability** | OpenTelemetry → Prometheus / Grafana |

## 📂 Repository structure (planned monorepo)

```
linkly/
├── apps/
│   ├── api/          # Spring Boot — management API (auth, links, domains, teams)
│   ├── resolver/     # Spring Boot — origin redirect service (the hot path)
│   ├── edge/         # Edge worker — global KV resolve + async click event
│   └── web/          # Next.js — dashboard, analytics, link-in-bio
├── infra/            # docker-compose (local), Terraform (cloud)
├── docs/             # ARCHITECTURE.md, ROADMAP.md
└── README.md
```

## 🚀 Getting started (local)

> Application code is being scaffolded per the [roadmap](./docs/ROADMAP.md). For now you can bring up the backing services:

```bash
# 1. Start Postgres, Redis, ClickHouse, Kafka
docker compose -f infra/docker-compose.yml up -d

# 2. Copy env template
cp .env.example .env    # then fill in secrets

# 3. (soon) run the API + resolver + web
#    cd apps/api && ./mvnw spring-boot:run
#    cd apps/web && npm install && npm run dev
```

Local services once up:

| Service | URL |
|---|---|
| Postgres | `localhost:5433` |
| Redis | `localhost:6379` |
| ClickHouse | `localhost:8123` (HTTP) |
| Kafka | `localhost:9092` |
| Kafka UI | http://localhost:8080 |

## 🗺️ Roadmap

A phased, day-by-day build plan (correctness → analytics → scale → platform) lives in **[`docs/ROADMAP.md`](./docs/ROADMAP.md)**.

## 📚 Documentation

- [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md) — diagrams + Architecture Decision Records (ADRs)
- [`docs/ROADMAP.md`](./docs/ROADMAP.md) — day-by-day build roadmap

## 📄 License

[MIT](./LICENSE) © Rajdeep Mandal
