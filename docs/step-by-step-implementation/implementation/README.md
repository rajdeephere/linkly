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
| 06+ | 6+ | Analytics, edge split, domains, routing, teams, flagship | ⬜ outlined per phase |

**✅ Phase 0 complete** (Days 1–5) — a correct, safe, fast, editable shortener on one service.

Companion: the **run/ship** track in [`../deployment/`](../deployment/).
