# Architecture Decision Records

Each ADR captures **one load-bearing decision** with a trade-off you can argue both sides of. They keep
settled decisions from being silently re-litigated — and each one is a ready-made answer to *"walk me
through a hard technical decision."*

**Format:** Status · Context · Decision · Consequences · Alternatives · Revisit-if.
**Status:** `Accepted` = decided & (being) implemented · `Proposed` = locked direction for a later phase.

| ADR | Decision | Status | Phase |
|-----|----------|--------|-------|
| [0001](./0001-resolver-separate-from-api.md) | Redirect resolver is a separate service from the management API | Proposed | 2 (Day 8) |
| [0002](./0002-kgs-base62-codes.md) | Short codes via a Key Generation Service (pre-allocated base62, non-enumerable) | Proposed | 1 (Day 3) |
| [0003](./0003-hybrid-edge-origin.md) | Hybrid edge KV + Spring Boot origin for resolve | Proposed | 2 (Day 8) |
| [0004](./0004-analytics-off-hot-path.md) | Analytics off the hot path: fire-and-forget → stream → OLAP | Proposed | 1 (Day 6) |
| [0005](./0005-302-not-301.md) | Default 302 (temporary) redirect, not 301 | Accepted | 0 (Day 2) |
| [0006](./0006-custom-domains-tls.md) | Multi-tenant custom domains + on-demand per-host TLS | Proposed | 3 (Day 10) |
| [0007](./0007-clickhouse-analytics.md) | ClickHouse (columnar OLAP) for click analytics | Proposed¹ | 1 (Day 6) |
| [0008](./0008-cache-invalidation.md) | Cache invalidation: write-through + explicit edge purge + short TTL | Proposed | 0→2 (Day 5/8) |
| [0009](./0009-safe-browsing-abuse.md) | Safe-Browsing scan on create + aggressive rate limits | Proposed | 0 (Day 4) |
| [0010](./0010-smart-routing.md) | Smart routing at resolve time; A-B with sticky bucketing | Proposed | 3 (Day 11) |
| [0011](./0011-nextjs-frontend.md) | Next.js App Router frontend, decoupled from the resolver | Accepted | 0 (Day 1) |
| [0012](./0012-flyway-schema.md) | Flyway-owned schema; `ddl-auto=validate` | Accepted | 0 (Day 1) |

¹ Postgres-first for the MVP; the ClickHouse split is the deliberate analytics-scale chapter.

**Where they come from:** 0001–0010 are the architecture-level decisions (mirrored from the project spec
§7 and [ARCHITECTURE.md](../ARCHITECTURE.md) §12); 0011–0012 are the concrete Day-1 implementation
decisions already shipped in the scaffold.
