# Impl 10 — Day 10: Branded custom domains

**Outcome:** tenants add a hostname → prove ownership via a DNS TXT record → get (simulated) per-host
TLS → serve branded links on it. Uniqueness is now **`(domain, code)`**, and the edge/resolver route by
the **`Host`** header. **Status:** ✅ shipped & verified. *(Starts Phase 3. QR moved to Day 11.)*

## Two halves
**A. api — domain management + `(domain, code)` uniqueness**
**B. resolver + edge — Host-based routing**

## Build order — Part A (`apps/api`)
1. **Migration** `V4__custom_domains.sql` — `domain` table; seed the **default domain** (`localhost:3000`,
   `is_default`); `link.domain_id` (backfill existing → default, then NOT NULL); drop `ux_link_code`,
   add unique **`(domain_id, code)`**.
2. **Domain** entity + `DomainRepository` (`findByHostname`); `Domain.DEFAULT_ID` constant.
3. **DNS** `DnsVerifier` interface + `StubDnsVerifier` — ownership "published" in-memory via a dev
   `simulatePublish` hook (stands in for the tenant's DNS provider); real impl does a TXT lookup at
   `_linkly-challenge.<host>` (ADR-0006).
4. **DomainService** + `DomainController` — `POST /v1/domains` (→ pending + DNS record),
   `GET /v1/domains/{id}`, `POST /v1/domains/{id}/verify` (DNS check → verified + TLS `pending→active`),
   `POST /v1/domains/{id}/dns/simulate` (dev-only sim hook).
5. **Link** gains `domain_id`; `LinkService.create` resolves the target domain (custom must be
   **verified** → else 409; else default), alias clash is **per-(domain, code)**, and `shortUrl` is built
   from the domain's hostname (`https://host/code`, or the base URL for the default). `LinkCache.evict`
   is now host-scoped (`link:{host}:{code}` + `edge:link:{host}:{code}`). Retired the Day-8 vestigial
   `resolve()`/`ResolveOutcome` from the api.

## Build order — Part B (`apps/resolver` + `apps/web`)
6. **resolver**: `Domain`(read) + `DomainRepository`; `Link` gains `domain_id`; `findByDomainIdAndCode`;
   `LinkCache` host-scoped; `ResolveService.resolve(code, host)` — `findByHostname(host)` → domain →
   link, unknown host → not-found. `RedirectController` uses the `Host` header; `ResolveApiController`
   takes `?host=`.
7. **edge** `middleware.ts`: `host = req.headers.host`; KV key `edge:link:{host}:{code}`; calls
   `/r/{code}?host={host}`.

## Verify
```bash
A=http://localhost:8081; E=http://localhost:3000
# domain lifecycle
curl -s -X POST $A/v1/domains -d '{"hostname":"go.acme.com"}' -H 'Content-Type: application/json'  # pending + DNS record
curl -s -X POST $A/v1/domains/$ID/verify        # 400 (not published)
curl -s -X POST $A/v1/domains/$ID/dns/simulate  # publish (sim)
curl -s -X POST $A/v1/domains/$ID/verify        # verified + TLS active
# host routing — same code, different Host
curl -sI -H 'Host: go.acme.com' $E/launch       # → acme.com
curl -sI -H 'Host: go.beta.com' $E/launch        # → beta.com
```
Verified: verify-before-publish **400**, publish→verify **verified+active**; `launch` coexists on acme
(→acme.com) and beta (→beta.com); **same code, different Host → different destination**; unknown host
**404**; default domain intact; edge KV host-scoped; edit **purges the host-scoped edge key** →
re-resolve serves NEW.

## Why (one line each)
`(domain, code)` uniqueness → two tenants can both own `/launch`. Verify-before-TLS → no cert-mining
(ADR-0006). Resolve keyed by Host → the right tenant's link, and the cache key must include the host or
two domains' codes collide. Unknown host → not-found (can't serve on an unregistered domain). Real DNS
lookup + ACME/on-demand TLS are the documented drop-ins for the two stubs.

## Trade-offs / caveats
- **TLS + DNS are simulated** locally (state transitions only) — real per-host certs need public DNS + a
  reachable host (Caddy on-demand / ACME); the interfaces (`DnsVerifier`, TLS status) are the seams.
- The `dns/simulate` endpoint is dev-only — production removes it (the real verifier replaces the stub).
- **Click analytics is still keyed by `code`** (not per-domain); with custom domains a rare alias
  collision across domains could merge counts — per-domain analytics is a follow-up.

## War-story (real, caught mid-verify)
Host routing worked but **nothing cached** — `edge:link:*` was empty despite correct 302s. Cause: I'd
started infra for Part 1 with `up -d postgres redis kafka` and **forgot `srh`**, so the edge KV backend
was down the whole time. The redirects still worked *because the miss path (resolver) is resilient* —
the outage was invisible to correctness, only killing the cache. Caught it because caching-should-happen
but didn't; started `srh`, re-verified. Lesson: a resilient system can **hide** a downed dependency —
watch for the *silent* symptom (no caching), not just errors. (See `linkly-docs` war-story #14.)

## Decisions referenced
- [ADR-0006 custom domains + TLS](../../adr/0006-custom-domains-tls.md) ·
  [ADR-0008 cache invalidation](../../adr/0008-cache-invalidation.md)

## Next
Day 11 — QR codes + smart routing (geo/device/OS/time + A-B).
