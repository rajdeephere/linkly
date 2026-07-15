# Impl 01 — Day 1: Monorepo scaffold + domain + boots-green

**Outcome:** the monorepo boots end-to-end — Spring Boot `apps/api` starts green against Postgres,
Flyway migrates `workspace`/`app_user`/`link` + seeds one link, `GET /ping` returns
`{"status":"ok","seededLinks":1}`, `/actuator/health` is `UP`, and Next.js `apps/web` production-builds
and live-checks the API. **Status:** ✅ shipped & verified.

## Prerequisites
- Java 21, Maven 3.9+ (or the bundled `./mvnw`), Node 24, Docker.
- Infra running — see [deployment/01-local-docker-dev](../deployment/01-local-docker-dev.md).

## Build order (backend — `apps/api`)
1. **Scaffold** `pom.xml` on `spring-boot-starter-parent` 3.3.5, Java 21. Deps: web, validation,
   data-jpa, data-redis, actuator, flyway-core + flyway-database-postgresql, postgresql, lombok, test.
   → *Parent BOM pins compatible versions.*
2. **Config** `application.yml` — datasource (Postgres **`:5433`** — see the port note below),
   `jpa.hibernate.ddl-auto=validate`, `open-in-view=false`, `flyway.enabled=true`, Redis url,
   actuator `health,info`, `server.port=8081`. → [ADR-0012](../../adr/0012-flyway-schema.md).
3. **Migration** `db/migration/V1__init.sql` — `workspace`, `app_user` (named `app_user` because
   `user` is reserved in Postgres), `link` (+ unique index on `code`, FK on `workspace_id`); seed one
   workspace + link. → schema owned by Flyway; Hibernate only validates.
4. **Domain** feature packages — `workspace/Workspace`, `user/User`, `link/Link` entities (UUID PKs via
   `GenerationType.UUID`; enums-as-string convention ready) + Spring Data repositories
   (`findByCode`, `existsByCode`). → *`(domain, code)` uniqueness is the plan; `code` is globally unique
   for now.*
5. **Smoke endpoint** `web/PingController` — `GET /ping` returns app name + status + `linkRepository.count()`
   (proves the DB round-trips). Real health at `/actuator/health`.
6. **Test** `LinklyApiApplicationTests.contextLoads()` — boots the context (needs infra up).

## Build order (frontend — `apps/web`)
1. Scaffold Next.js 14 (App Router) + Tailwind + shadcn config (`components.json`, `cn()` util,
   CSS-variable theme in `globals.css`). → [ADR-0011](../../adr/0011-nextjs-frontend.md).
2. `next.config.mjs` rewrites `/api/*` → `http://localhost:8081` (dev proxy to the API).
3. `lib/api.ts` — `getApiStatus()` hits `GET /ping` (try/catch → `{ok:false}`), `cache: "no-store"`.
4. `app/page.tsx` — a **server component** that renders the live API status; `app/layout.tsx` (dark).

## Verify
```bash
# infra
docker compose -f infra/docker-compose.yml up -d postgres redis
# backend
cd apps/api && ./mvnw -q -DskipTests spring-boot:run   # boots on :8081
curl -s http://localhost:8081/ping                     # {"status":"ok","seededLinks":1,"app":"linkly-api"}
curl -s http://localhost:8081/actuator/health          # {"status":"UP"}   (Postgres + Redis up)
# frontend
cd apps/web && npm install && npm run build             # ✓ compiled; / is dynamic (no-store)
```

## Port note (real Day-1 gotcha)
This machine runs a **native Postgres on `localhost:5432`**, which shadowed the container and caused
`FATAL: password authentication failed for user "linkly"` at first boot. Fixed by mapping the container
to host **5433** (compose + `application.yml`). See the war-story in
`linkly-docs/interview-prep/war-stories.md`.

## Why (one line each)
Flyway + validate → schema is reviewed code, not ORM guesswork ([ADR-0012](../../adr/0012-flyway-schema.md)).
UUID PKs → non-enumerable ids, no central sequence (multi-region-friendly). `app_user` → avoids the
reserved `user` keyword. Separate api/web with a dev proxy → clean contract, zero shared runtime
([ADR-0011](../../adr/0011-nextjs-frontend.md)). `open-in-view=false` → no hidden lazy-load N+1s.

## Decisions referenced
- [ADR-0011 Next.js frontend](../../adr/0011-nextjs-frontend.md) ·
  [ADR-0012 Flyway schema](../../adr/0012-flyway-schema.md)
- Looking ahead: [ADR-0001 resolver split](../../adr/0001-resolver-separate-from-api.md),
  [ADR-0005 302 redirect](../../adr/0005-302-not-301.md) (both land Day 2+)
