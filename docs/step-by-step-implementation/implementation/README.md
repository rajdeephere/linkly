# Implementation runbooks (Build)

The hands-on **build** track — how to *write* each feature. One runbook per build day; detailed when the
day ships, outlined before.

| # | Day | Topic | Status |
|---|-----|-------|--------|
| [01](./01-day1-scaffold.md) | 1 | Monorepo scaffold + domain + boots-green | ✅ full |
| [02](./02-day2-shorten-resolve.md) | 2 | Shorten (`POST /v1/links`) + `GET /{code}` 302 resolve + shorten bar | ✅ full |
| [03](./03-day3-kgs.md) | 3 | KGS base62 non-enumerable codes | ✅ full |
| [04](./04-day4-aliases-expiry-safety.md) | 4 | Custom aliases, expiry, Safe-Browsing, rate limit | ✅ full |
| [05](./05-day5-edit-delete-cache.md) | 5 | Edit/delete + Redis cache-aside + invalidation | ✅ full |
| [06](./06-day6-analytics-pipeline.md) | 6 | Analytics pipeline — click events → Kafka → Postgres | ✅ full |
| [07](./07-day7-analytics-dashboard.md) | 7 | Analytics query API + dashboard | ✅ full |
| [08](./08-day8-edge-resolver-split.md) | 8 | Resolver split + edge (Vercel KV) hot path | ✅ full |
| 09+ | 9+ | Load test/resiliency, domains, routing, teams, flagship | ⬜ outlined per phase |

**✅ Phase 0 complete** (Days 1–5). **✅ Phase 1 complete** (Days 6–7). **Phase 2 in progress** — Day 8 ✅ (split + edge), Day 9 next (load test + origin-down).

Companion: the **run/ship** track in [`../deployment/`](../deployment/).
