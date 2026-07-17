# Implementation runbooks (Build)

The hands-on **build** track — how to *write* each feature. One runbook per build day; detailed when the
day ships, outlined before.

| # | Day | Topic | Status |
|---|-----|-------|--------|
| [01](./01-day1-scaffold.md) | 1 | Monorepo scaffold + domain + boots-green | ✅ full |
| [02](./02-day2-shorten-resolve.md) | 2 | Shorten (`POST /v1/links`) + `GET /{code}` 302 resolve + shorten bar | ✅ full |
| 03 | 3 | KGS base62 non-enumerable codes | ⬜ outlined |
| 04 | 4 | Custom aliases, expiry, Safe-Browsing, rate limit | ⬜ outlined |
| 05 | 5 | Edit/delete + Redis cache-aside + invalidation | ⬜ outlined |
| 06+ | 6+ | Analytics, edge split, domains, routing, teams, flagship | ⬜ outlined per phase |

Companion: the **run/ship** track in [`../deployment/`](../deployment/).
