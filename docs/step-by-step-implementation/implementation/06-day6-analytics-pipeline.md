# Impl 06 — Day 6: Analytics pipeline (off the hot path)

**Outcome:** every resolve emits a click event **fire-and-forget** → Kafka → an enrichment consumer →
Postgres `click_event`. Proven: with Kafka stopped, redirects still return 302 in ~ms. **Status:** ✅
shipped & verified. *(Starts Phase 1.)*

## Prerequisites
- Phase 0 done; infra up **incl. Kafka**.

## Design (ADR-0004)
```
resolve → publish(ClickEventMessage)   [@Async, off the request thread]
        → Kafka topic link.clicks
        → @KafkaListener consumer (group linkly-analytics): enrich → save
        → Postgres click_event   (Postgres-first; ClickHouse later — ADR-0007)
```
Raw {code, ip, ua, referer, country, ts} on the wire; the **consumer** hashes the IP (GDPR), parses the
UA (device/os/browser), flags bots.

## Build order (backend — `apps/api`)
1. **Dep** `spring-kafka`; **migration** `V3__click_event.sql` (denormalized `link_code`, `ip_hash`,
   `country`, `device`, `os`, `browser`, `referer`, `is_bot`; index on `(link_code, ts)`).
2. **Domain** `analytics/ClickEvent` (entity) + `ClickEventRepository`.
3. **Message** `analytics/ClickEventMessage` (record; `ts` epoch-millis for trivial JSON).
4. **Publisher** `analytics/ClickEventPublisher` — `@Async("clickExecutor")` `kafka.send`, try/catch so a
   broker outage fails on the background thread, never the request. Topic `link.clicks`.
5. **Consumer** `analytics/ClickEventConsumer` — `@KafkaListener(group linkly-analytics)` → hash IP
   (SHA-256), `UserAgentParser` → device/os/browser/bot → `repo.save`.
6. **Parser** `analytics/UserAgentParser` — dependency-free heuristic (prod → a UA library).
7. **Config** `config/AsyncConfig` (`@EnableAsync` + bounded `clickExecutor`, DiscardPolicy under flood);
   `analytics/AnalyticsConfig` (`NewTopic`); `common/ClientIp` (shared X-Forwarded-For util, used by
   both controllers); `application.yml` Kafka producer/consumer (JSON, `max.block.ms=2000`).
8. **Wire** `RedirectController` publishes on a `REDIRECT` outcome with request metadata.

## Infra
`infra/docker-compose.yml` Kafka switched to **`apache/kafka:3.9.0`** (the `bitnami/kafka:3.7` tag was
pulled from the registry) with **dual listeners**: `EXTERNAL://localhost:9092` (host-run app) +
`INTERNAL://kafka:29092` (in-network: kafka-ui, the `api` container). `api` gains
`KAFKA_BOOTSTRAP_SERVERS=kafka:29092` + a `kafka` health dependency.

## Verify
```bash
# create + resolve 5× (browser UA) + 1× Googlebot
for n in 1 2 3 4 5; do curl -s -o /dev/null -A "Mozilla/5.0 … Chrome/120" $B/$CODE; done
curl -s -o /dev/null -A "Googlebot/2.1" $B/$CODE
docker exec linkly-postgres psql -U linkly -d linkly -c \
  "select device,os,browser,is_bot,left(ip_hash,12) from click_event where link_code='$CODE'"
# outage proof
docker stop linkly-kafka; curl -s -o /dev/null -w '%{http_code} %{time_total}s\n' $B/$CODE  # 302 ~ms
```
Verified: 6 events landed enriched (browser → Desktop/Windows/Chrome/false; Googlebot → **is_bot=true**;
IP **hashed**). **Kafka stopped → redirects still 302 in 5–17ms** (no error, no slowdown).

## Why (one line each)
`@Async` publish → the resolve path never blocks on (or fails with) analytics. Fire-and-forget + a
separate consumer group → the pipeline can lag/restart/scale independently (ADR-0004). Hash IP in the
consumer → no raw PII at rest (GDPR). Denormalized `link_code` → analytics needs no join (ADR-0007).
Dual Kafka listeners → same broker reachable from host *and* in-network.

## Trade-offs noted
Per-event insert for now (batch inserts are the scale step). `country` comes from a `CF-IPCountry`
header (set by the edge in Phase 2); null until then — real GeoIP (MaxMind) is the alternative.

## Decisions referenced
- [ADR-0004 analytics off hot path](../../adr/0004-analytics-off-hot-path.md) ·
  [ADR-0007 ClickHouse/Postgres-first](../../adr/0007-clickhouse-analytics.md)

## Next
Day 7 — the analytics **dashboard**: aggregation queries + a time-series/geo/device view.
