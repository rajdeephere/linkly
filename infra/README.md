# Linkly — Infra (local)

`docker-compose.yml` runs the whole local stack: the **backing services** (Postgres, Redis, ClickHouse,
Kafka + Kafka UI) and the **application services** (`api`, `web`), each built from `../apps/*`.

## Services

| Service | Host port | Image / build | Notes |
|---------|-----------|---------------|-------|
| `postgres` | **5433** → 5432 | postgres:16 | remapped off 5432 to coexist with a native install |
| `redis` | 6379 | redis:7 | cache / KGS pool / rate limits |
| `clickhouse` | 8123, 9000 | clickhouse:24 | analytics store (Phase 1; Postgres-first before that) |
| `kafka` | 9092 | bitnami/kafka (KRaft) | click-event stream (Phase 1) |
| `kafka-ui` | 8080 | provectuslabs/kafka-ui | browse topics |
| `api` | 8081 | build `../apps/api` | Spring Boot management API + resolver |
| `web` | 3000 | build `../apps/web` | Next.js — proxies `/api/*` → `api:8081` in-network |

App data persists under `infra/data/**` (gitignored).

## Two ways to run

**A. Full stack in containers** (nothing on the host but Docker):
```bash
docker compose -f infra/docker-compose.yml up -d --build
# web → http://localhost:3000 · api → http://localhost:8081
```

**B. Backing services in Docker, apps on the host** (fast inner loop for coding):
```bash
docker compose -f infra/docker-compose.yml up -d postgres redis     # (+ clickhouse kafka when needed)
cd apps/api && ./mvnw spring-boot:run          # :8081
cd apps/web && npm run dev                     # :3000
```

## Networking notes
- Inside the compose network, services use **container hostnames + internal ports** (`postgres:5432`,
  `redis:6379`, `api:8081`) — the `5433` host remap is only for host access.
- `web` sets `API_URL=http://api:8081` (build arg **and** runtime env): the build bakes it into
  Next's `/api/*` rewrite, and the server-side status fetch reads it at runtime.
- `api` sets `LINKLY_BASE_URL=http://localhost:8081` so generated short URLs are **browser**-reachable.

## Teardown
```bash
docker compose -f infra/docker-compose.yml stop        # keep data
docker compose -f infra/docker-compose.yml down -v      # wipe containers + volumes
```
