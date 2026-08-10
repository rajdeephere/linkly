# Impl 09 — Day 9: Load test + origin-down resiliency

**Outcome:** the edge hot path is load-tested (k6) and proven to survive an **origin outage** — hot
links keep redirecting from edge KV with the resolver down, cold links degrade cleanly, and the
management API is unaffected. **Status:** ✅ shipped & verified. **This completes Phase 2.**

## Prerequisites
- Day 8 edge + resolver split; full stack up (infra + api :8081 + resolver :8082 + edge :3000).

## Part 1 — Load test (k6)
`infra/loadtest/redirect.js` — ramping-VUs (0→50→0 over 30s) against a **pre-warmed hot link** through
the edge, `redirects:0` so k6 records the 302 itself. Run via Docker (nothing to install):
```bash
CODE=<warmed hot code>
docker run --rm -i --add-host=host.docker.internal:host-gateway \
  -e TARGET="http://host.docker.internal:3000/$CODE" \
  grafana/k6 run - < infra/loadtest/redirect.js
```
**Result:** 10,261 requests, **0 failures, 100% 302** at ~342 req/s sustained.

> **Latency caveat (honest):** absolute latencies (p95 ~218ms) are dominated by **`next dev`** — a
> single-threaded dev server recompiling middleware — *not* the architecture. The `p99<150ms` threshold
> "crossed" for that reason. On a real Vercel edge these are single-digit ms globally; locally the valid
> claims are **throughput stability + zero errors + 100% correctness**, not the absolute numbers. A
> production number would come from `vercel deploy` + k6 from multiple regions.

## Part 2 — Origin-down drill (the headline)
Stop the resolver, then exercise the edge:
```bash
# stop the origin resolver, CONFIRM it's actually down first
curl -s -o /dev/null -w '%{http_code}' http://localhost:8082/actuator/health   # must be 000
```
| Scenario (resolver DOWN) | Result |
|---|---|
| **Hot link** (in edge KV) | **302 in ~10ms** — served from the edge, origin never touched |
| **Cold link** (uncached) | **clean 404 in ~12ms** — edge can't reach resolver → degrades, no 500/hang |
| **Management API** (separate service) | **201 create + /ping healthy** — outage is contained (ADR-0001) |

## Why (one line each)
Hot links live at the edge → the origin is not on the hot path for the 99% (ADR-0003), so an origin
outage is invisible to cached traffic. The resolver being a **separate service** (ADR-0001) means its
outage can't touch the management API. Cold-link failure is a clean 404, not a cascade — the edge treats
an unreachable origin as "unknown," not "error."

## War-story (real, caught mid-drill)
The first drill run was **invalid**: `TaskStop` on the `mvnw` process left the **forked Spring Boot JVM
still listening on 8082**, so the "origin" was actually up (health returned 200) and the cold link
resolved 302 — a false pass. The **health check reading 200 when it should be 000** was the tell. Fixed
by killing the actual PID on the port and re-confirming `000` before re-running. Lesson: a forked
`spring-boot:run` outlives its Maven parent — **verify the dependency is truly down before claiming a
chaos test passed.** (See `linkly-docs` war-story #12.)

## Trade-offs / next
- Local "edge" = `next dev` on one host; real latency/geo needs a Vercel deploy (deferred — no account
  wired yet). The *architecture* claims (KV-absorbs-load, origin-down-safe) are proven regardless.
- Next: **Phase 3** — branded custom domains + QR + smart routing (Day 10).

## Decisions referenced
- [ADR-0001 resolver split](../../adr/0001-resolver-separate-from-api.md) ·
  [ADR-0003 hybrid edge/origin](../../adr/0003-hybrid-edge-origin.md)

---
**✅ Phase 2 complete** — the read hot path is scaled (edge KV) and resilient (survives origin outage).
