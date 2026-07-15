# Deploy 01 — Local docker dev

**Outcome:** the backing services run in Docker; `apps/api` and `apps/web` run on the host against them.
**Status:** ✅ working (Day 1 verified with Postgres + Redis).

## Services (`infra/docker-compose.yml`)
| Service | Host port | Notes |
|---------|-----------|-------|
| Postgres 16 | **5433** → 5432 | remapped off 5432 to coexist with a native Postgres install |
| Redis 7 | 6379 | cache-aside / KGS pool / rate limits (used Day 5+) |
| ClickHouse 24 | 8123 (HTTP), 9000 | analytics store (used Phase 1; Postgres-first before that) |
| Kafka 3.7 (KRaft) | 9092 | click-event stream (used Phase 1) |
| Kafka UI | 8080 | browse topics |

Data persists under `infra/data/**` (gitignored).

## Bring it up
```bash
cd "D:/Personal Project/code/linkly"

# start everything (or name specific services)
docker compose -f infra/docker-compose.yml up -d
# Day 1 only needs Postgres + Redis:
docker compose -f infra/docker-compose.yml up -d postgres redis

# wait for Postgres health
docker inspect -f '{{.State.Health.Status}}' linkly-postgres   # → healthy
```

## Run the apps (host)
```bash
# API — http://localhost:8081
cd apps/api && ./mvnw -q -DskipTests spring-boot:run

# Web — http://localhost:3000  (proxies /api/* → :8081)
cd apps/web && npm install && npm run dev
```

## Verify
```bash
curl -s http://localhost:8081/ping             # {"status":"ok","seededLinks":1,...}
curl -s http://localhost:8081/actuator/health  # {"status":"UP"} with Postgres+Redis up
```

## Config
Defaults live in `apps/api/src/main/resources/application.yml`; override via env (`POSTGRES_URL`,
`REDIS_URL`, …). Copy `.env.example` → `.env` for secrets. Ports chosen to avoid clashes: API `8081`
(Kafka UI owns `8080`), web `3000`, Postgres `5433`.

## Teardown
```bash
docker compose -f infra/docker-compose.yml stop        # keep data
docker compose -f infra/docker-compose.yml down -v      # wipe data + volumes
```

## Gotcha (Day 1)
A **native Postgres on host 5432** shadowed the container → `password authentication failed`. Container
is mapped to **5433** to sidestep it without touching the host install. Full story in
`linkly-docs/interview-prep/war-stories.md`.
