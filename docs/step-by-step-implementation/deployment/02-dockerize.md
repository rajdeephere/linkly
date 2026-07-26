# Deploy 02 — Dockerize api + web (full local stack in compose)

**Outcome:** `api` and `web` build as images and run as compose services alongside the backing services,
so `docker compose up -d --build` brings up the **whole stack** — no host toolchain needed.
**Status:** ✅ working (verified end-to-end through the containers).

## Images
- **`apps/api/Dockerfile`** — multi-stage: `maven:3.9-eclipse-temurin-21` builds the jar
  (`mvn -DskipTests clean package`) → `eclipse-temurin:21-jre` runs it. Exposes 8081.
- **`apps/web/Dockerfile`** — multi-stage: `node:22-alpine` runs `npm ci` + `npm run build` (Next
  **standalone** output) → minimal `node:22-alpine` runs `node server.js`. Exposes 3000.
  `next.config.mjs` sets `output: "standalone"`.
- `.dockerignore` in each keeps the build context small (`target/`, `node_modules/`, `.next/`).

## Compose wiring (`infra/docker-compose.yml`)
```yaml
api:
  build: { context: ../apps/api }
  depends_on: { postgres: {condition: service_healthy}, redis: {condition: service_healthy} }
  environment:
    POSTGRES_URL: jdbc:postgresql://postgres:5432/linkly   # in-network host + container port
    REDIS_URL: redis://redis:6379
    LINKLY_BASE_URL: http://localhost:8081                 # browser-reachable short-URL base
  ports: ["8081:8081"]

web:
  build: { context: ../apps/web, args: { API_URL: http://api:8081 } }
  depends_on: [api]
  environment: { API_URL: http://api:8081 }                # runtime status fetch
  ports: ["3000:3000"]
```
A `redis` healthcheck was added so `api` can wait on it (Postgres already had one).

## The two networking gotchas
1. **In-network vs host ports.** Inside the compose network, `api` reaches Postgres at
   `postgres:5432` (the container port) — **not** the `5433` host remap, which only exists for host
   access.
2. **`API_URL` is needed twice for `web`.** Next evaluates `rewrites()` at **build** time (so the
   `/api/*` proxy target is baked in) *and* the server-side status fetch reads `process.env.API_URL` at
   **runtime** — so it's both a build `arg` and a runtime `environment` value, both `http://api:8081`.

## Verify
```bash
docker compose -f infra/docker-compose.yml up -d --build
curl -s http://localhost:8081/ping                                   # {"status":"ok",...,"kgsPool":...}
curl -s -X POST http://localhost:3000/api/v1/links \
  -H 'Content-Type: application/json' -d '{"destinationUrl":"https://claude.com/"}'   # 201 (web → api)
curl -s -o /dev/null -D - http://localhost:8081/<code> | grep -i location             # 302
```
Verified: page served by `web`, create proxied `web → api`, returned link resolves via `api`.

## Gotcha hit during setup
A stray host `spring-boot:run` java process (from an earlier host-mode run) still held **8081**, so the
`api` container couldn't bind it (`ports are not available … 8081`). Killing the orphan freed the port —
a forked Spring Boot app can outlive the `mvnw` that launched it. Pick **one** run mode at a time
(containers *or* host), not both on the same ports.

## Next
Day 8 splits `api` into `api` + `resolver` and adds the edge tier + (multi-pod) — this compose file
grows accordingly (see DEPLOYMENT-ARCHITECTURE.md).
