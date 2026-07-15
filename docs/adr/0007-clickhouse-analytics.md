# ADR-0007 — ClickHouse (columnar OLAP) for click analytics

**Status:** Proposed (implement Phase 1 / Day 6; Postgres-first for MVP) · **Date:** 2026-07-15

## Context
Click analytics are `COUNT / SUM / GROUP BY country, day, device` over an append-only fact table that
can reach billions of rows. Metadata (links, domains, rules) is relational and transactional. One store
can't be great at both.

## Decision
**Polyglot persistence.** Click events go to **ClickHouse** (columnar OLAP); metadata stays in
**PostgreSQL** (row store, OLTP). The event table is denormalized (workspace/link fields copied in) so
no cross-store join is needed at query time.

## Consequences
- Aggregations scan only the columns they need and compress heavily → fast over huge tables.
- Two stores to operate; no in-query join back to Postgres (denormalize instead).
- ClickHouse is insert-and-read, not update-in-place — fine, because clicks are immutable.

## Alternatives
- **Postgres/Timescale**: fine to low-millions of clicks — the deliberate **MVP shortcut** (start here).
- **Managed Tinybird** (ClickHouse under the hood — what Dub uses): less to run, less to learn.

## Revisit if
MVP volume stays small → stay on Postgres; introduce ClickHouse only when aggregate queries slow down.
Pairs with [ADR-0004](./0004-analytics-off-hot-path.md) (the pipeline that feeds it).
