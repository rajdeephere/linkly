# Phase 01 — Analytics off the hot path

**Status:** ⬜ next · **Roadmap:** Days 6–7

## Goal
Capture every click as rich, queryable analytics **without ever touching the redirect hot path**, and
surface it in a genuinely good dashboard.

## Scope
**In:** fire-and-forget click event on resolve → stream (Kafka) → enrichment consumer (geo/device/
browser/bot flag, IP hashed for GDPR) → analytics store; aggregation queries (timeseries, byCountry,
byDevice, byReferrer, top links); dashboard with time-series chart, geo map, breakdowns, date-range.
**Out:** edge resolve (Phase 2), custom domains, routing. Analytics store starts **Postgres-first**;
the ClickHouse split lands when aggregate queries slow.

## Architecture delta
```
   resolver ──(non-blocking)──► Kafka ──► enrich consumer ──► analytics store (Postgres → ClickHouse)
   management api ──query──► dashboard (Next.js: chart · map · breakdowns)
```
Off the hot path ([ADR-0004](../adr/0004-analytics-off-hot-path.md)); store choice
([ADR-0007](../adr/0007-clickhouse-analytics.md)).

## Done when
- [ ] **Day 6:** click event streamed after redirect; consumer enriches + inserts; **killing the
      consumer does not affect redirects**; IP hashing + retention configured.
- [ ] **Day 7:** dashboard shows clicks-over-time, geo map, device/browser/referrer, top links.

## Maps to
- ADRs: [0004 analytics off hot path](../adr/0004-analytics-off-hot-path.md),
  [0007 ClickHouse](../adr/0007-clickhouse-analytics.md)
- Hard scenarios: analytics-store-down (#4), bot filtering (#10), GDPR (#14)
