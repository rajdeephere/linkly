# ADR-0012 — Flyway-owned schema; `ddl-auto=validate`

**Status:** Accepted (Day 1) · **Date:** 2026-07-15

## Context
Hibernate can auto-manage the schema (`ddl-auto=update`), which is convenient and dangerous: changes
aren't reviewed, aren't versioned, differ across environments, can't express data migrations or
rollbacks, and can silently lock/rewrite tables in prod.

## Decision
The schema is **versioned SQL owned by Flyway** (`V1__init.sql`, …). Hibernate runs with
**`ddl-auto=validate`** — it only *asserts* its entity mappings match the real schema at boot.

## Consequences
- Schema changes are reviewable in a PR, deterministic, ordered, and identical everywhere.
- Entity/schema drift is caught as a **startup failure**, not a runtime surprise.
- Every schema change is an explicit migration — slightly more ceremony, much more safety.

## Alternatives
- **`ddl-auto=update`**: convenient for prototyping, unsafe and non-reproducible for real deploys.
- **`ddl-auto=none` + manual DDL**: loses the boot-time validation safety net.

## As built (Day 1)
`V1__init.sql` creates `workspace`, `app_user`, `link` (+ unique index on `code`, FK on `workspace_id`)
and seeds one workspace + link, so `/ping` returns `seededLinks: 1`. Boot verified: Flyway migrated and
Hibernate validated the three entities against the schema.

## Revisit if
Never for correctness. If migrations get large, add Flyway callbacks / repeatable migrations for views.
