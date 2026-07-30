# Phase 01 — Analytics off the hot path

**Status:** ✅ complete (Days 6–7) · **Roadmap:** Days 6–7

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
- [x] **Day 6:** click event streamed (fire-and-forget) after redirect; consumer enriches (UA→device/os/
      browser, bot flag, hashed IP) + inserts to Postgres; **Kafka stopped → redirects still 302 in ~ms**.
- [x] **Day 7:** `GET /v1/links/{id}/analytics` (totals + human/bot split + daily timeseries +
      device/browser/country/referrer); dashboard renders it with a 7/30/90-day picker. *(Geo = Unknown
      until edge-provided, Phase 2.)*

**✅ Phase 1 complete** — clicks captured off the hot path and surfaced in a dashboard.

## Maps to
- ADRs: [0004 analytics off hot path](../adr/0004-analytics-off-hot-path.md),
  [0007 ClickHouse](../adr/0007-clickhouse-analytics.md)
- Hard scenarios: analytics-store-down (#4), bot filtering (#10), GDPR (#14)
