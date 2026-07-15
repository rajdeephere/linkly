# Linkly — Deployment Architecture

How Linkly is run and shipped. The load-bearing idea: the **hot redirect path and the management path
deploy and scale independently** ([ADR-0001](./adr/0001-resolver-separate-from-api.md)), and hot links
are served from a **global edge** with a Spring Boot **origin** as source of truth
([ADR-0003](./adr/0003-hybrid-edge-origin.md)).

## Topology

```
                 ┌─────────── Global edge (Cloudflare Workers / Vercel Edge) ───────────┐
   Visitors ───► │  Edge resolver + KV (hot links)  ·  Next.js web + link-in-bio        │
                 └───────────────┬─────────────────────────────────┬────────────────────┘
                        miss     │                          events  │
                                 ▼                                  ▼
   ┌──────────────── AWS region ─────────────────────────────────────────────────────────┐
   │  ALB ─► ECS/EKS:  resolver pods (autoscaled, read-heavy)                              │
   │                   api pods (management)                                              │
   │                   analytics-consumer pods                                            │
   │  RDS Postgres (Multi-AZ) · ElastiCache Redis · MSK/Kinesis · ClickHouse · S3         │
   │  OpenTelemetry → Prometheus / Grafana                                                │
   └───────────────────────────────────────────────────────────────────────────────────────┘
```

## Independent scaling
| Tier | Scales on | Notes |
|------|-----------|-------|
| Edge resolver | global request volume | absorbs viral spikes; origin shielded |
| Origin resolver | cache-miss rate | tiny, cache-first; autoscale on CPU/RPS |
| Management API | admin/API traffic | never contends with the hot path |
| Analytics consumer | event lag | scale by Kafka consumer-group partitions |

## Rollout strategy
- **Stateless services** (api, resolver, consumers) → rolling deploy behind the ALB; readiness probes
  gate traffic. No sticky state to drain.
- **Edge** deploys via the provider (Workers/Vercel) — versioned, instant rollback.
- **Schema** changes ship as **Flyway migrations** ([ADR-0012](./adr/0012-flyway-schema.md)) ahead of
  the code that needs them (expand/contract).
- **Cache** correctness on deploy is handled by write-through + purge, not deploy-time flushes
  ([ADR-0008](./adr/0008-cache-invalidation.md)).

## CI/CD
GitHub Actions: `build → test → image → deploy`. Terraform for infra (RDS, ElastiCache, MSK, ECS/EKS,
IAM). Edge + web deploy through the provider pipeline. Secrets from a secrets manager / SSM — never the
repo (`.env` is gitignored; `.env.example` documents the keys).

## Scaling & DR
- **Read scaling** is the whole game: edge KV + Redis cache-aside keep Postgres reads minimal.
- **Postgres** Multi-AZ (sync standby); PITR backups. **ClickHouse** replicated for analytics durability.
- **Edge** is inherently multi-region; the origin starts single-region, with multi-region as the
  Phase-5 stretch (geo-routed origins + replicated KV warm-up).
- **Degradation:** origin down → edge serves last-known KV (stale-while-revalidate); analytics down →
  redirects unaffected (fire-and-forget, [ADR-0004](./adr/0004-analytics-off-hot-path.md)).

## Local ↔ cloud parity
Local dev uses `infra/docker-compose.yml` (Postgres `:5433`, Redis, ClickHouse, Kafka) — see
[step-by-step-implementation/deployment/01-local-docker-dev.md](./step-by-step-implementation/deployment/01-local-docker-dev.md).
The cloud swaps these for RDS / ElastiCache / MSK / ClickHouse but keeps the same service boundaries.

## Ports (local)
API `8081` · web `3000` · Postgres `5433` (native install owns `5432`) · Redis `6379` ·
ClickHouse `8123/9000` · Kafka `9092` · Kafka UI `8080`.
