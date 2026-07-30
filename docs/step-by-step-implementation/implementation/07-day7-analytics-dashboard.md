# Impl 07 — Day 7: Analytics dashboard

**Outcome:** `GET /v1/links/{id}/analytics?days=N` returns aggregated click stats; a Next.js dashboard
renders totals, a clicks-over-time chart, and device/browser/referrer/country breakdowns. **Status:** ✅
shipped & verified. **This completes Phase 1.**

## Prerequisites
- Day 6 pipeline ([06-day6-analytics-pipeline](./06-day6-analytics-pipeline.md)); infra up.

## Build order (backend — `apps/api`)
1. **Aggregation queries** in `ClickEventRepository` (native SQL, bounded by `ts >= :since`):
   `totals` (human/bot split via `count(*) filter (where …)`), `timeseriesDaily`
   (`to_char(date_trunc('day', ts))` → `[YYYY-MM-DD, count]`), and `byDevice` / `byBrowser` /
   `byCountry` / `byReferrer` (`group by … order by count desc`, referrers `limit 10`).
2. **DTO** `analytics/dto/AnalyticsResponse` — totals + `days` + `timeseries[]` + four `Bucket[]`.
3. **Service** `AnalyticsService.forLink(id, days)` — resolve id→link→code (404 if missing),
   `since = now - days`, run the queries, map rows (`((Number) r[1]).longValue()`).
4. **Endpoint** `AnalyticsController` — `GET /v1/links/{id}/analytics?days=30`.

## Build order (frontend — `apps/web`)
1. `components/analytics.tsx` — `StatTile`, `ColumnChart` (daily), `BarList` (category). Per the
   dataviz method: each chart is **single-series → single accent hue, no legend**, identity via text
   labels, recessive tracks, 4px rounded data-ends, hover tooltips; theme-aware surfaces.
2. `app/analytics/[id]/page.tsx` — client page: `days` selector (7/30/90), fetches
   `/api/v1/links/{id}/analytics`, renders the stat row + charts. Notes the geo caveat inline.
3. `components/shortener.tsx` — the create result now links **“View analytics →”** to `/analytics/{id}`.

## Verify
```bash
# generate varied clicks (Chrome/Firefox/Safari-iPhone/Edge + Googlebot, some with a Referer)
curl -s "$B/v1/links/$ID/analytics?days=30"
```
Verified against 11 generated clicks: `total 11 · human 10 · bot 1`; devices Desktop 9 / Mobile 2;
browsers Chrome 4 / Firefox 3 / Safari 2 / Edge 1 / Other 1 (bot → Other); referrers direct 4 /
twitter 4 / HN 3; countries Unknown 11. Web builds; `/analytics/[id]` route present.

## Why (one line each)
Aggregate in SQL (`group by`, `date_trunc`, `filter`) → the DB does the counting, the API just shapes
it. Bot split via `filter` → crawler hits don't inflate "real" numbers. Single-hue charts + text-labeled
identity → no categorical palette to validate; readable in light and dark. `days` bounds every query →
one cheap knob for the range picker.

## Trade-offs noted
- Per-request aggregation over Postgres is fine at MVP volume; the ClickHouse split (ADR-0007) is the
  scale answer, and materialized rollups the next step after that.
- Timeseries returns only days with clicks (gap-filling zero-days is a UI nicety, deferred).
- `country` is "Unknown" until the edge provides `CF-IPCountry` (Phase 2) or a GeoIP DB is added.

## Decisions referenced
- [ADR-0007 ClickHouse / Postgres-first](../../adr/0007-clickhouse-analytics.md) ·
  [ADR-0004 analytics off hot path](../../adr/0004-analytics-off-hot-path.md)

---
**✅ Phase 1 complete** — clicks captured off the hot path *and* surfaced. Next arc: **Phase 2**, the
read hot path at scale (edge + resolver split, Day 8).
