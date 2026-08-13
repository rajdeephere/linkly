# Impl 11 — Day 11: QR codes + smart routing

**Outcome:** dynamic QR codes per link (survive a destination edit) and resolve-time **smart routing** —
device/OS deep links, geo targeting, and weighted **A-B splits with sticky bucketing**. **Status:** ✅
shipped & verified. **This completes Phase 3.**

## Part 1 — QR codes (`apps/api`)
- **ZXing** (`com.google.zxing:core` + `:javase`); `QrService.png(text, size)`; `QrController`
  `GET /v1/links/{id}/qr?size=` → `image/png`.
- The QR encodes the link's **short URL** (via `LinkService.shortUrl`), *not* the destination — so
  editing where the link points leaves every printed QR working. Generated on demand; nothing stored.
- **Verified:** valid PNG (`89504e47` magic, `image/png`); QR bytes **identical** before/after a
  destination edit.

## Part 2 — Smart routing (evaluated in `apps/resolver`, ADR-0010)
1. **api** — `V5__routing_rules.sql` (`routing_rule`: type DEVICE|OS|GEO|AB, `match_value`,
   `destination_url`, `weight`, `priority`); `RoutingRule` entity + repo; `RoutingService.add` (validates
   the link, **evicts its cache** — a ruled link is no longer plain-cacheable); `RoutingController`
   `POST/GET /v1/links/{id}/rules`.
2. **resolver** — `RoutingRule`(read) + repo; `RoutingContext.from(ua, country, ip)` (parses device/OS
   from the UA, bucketKey = `ip|ua`); `RoutingEvaluator.pick`:
   - DEVICE/OS/GEO rules by `priority` asc → **first match wins** (deep links);
   - else weighted **A-B**, bucketed **deterministically** by `bucketKey` → a returning visitor always
     gets the same variant;
   - else the link's default destination.
   `ResolveService.resolve(code, host, ctx)` loads rules on a cache miss; **a ruled link is never
   cached** (`cacheable=false`), so every hit re-evaluates against the live request.
3. **resolver entry points** — `GET /{code}` builds the context from its own headers;
   `GET /r/{code}?host=&ua=&country=&ip=` from params.
4. **edge** — forwards `ua`, `country`, `ip` to `/r` so the resolver can evaluate per request.

## Verify
```bash
# deep links
curl -s -X POST $A/v1/links/$ID/rules -d '{"type":"OS","matchValue":"iOS","destinationUrl":"https://apps.apple.com/app"}' -H 'Content-Type: application/json'
curl -sI -A '<iPhone UA>' $E/$CODE     # → apps.apple.com  ; Android → play ; Desktop → fallback
# A-B: same UA repeated → same variant; varied UA → distributes
curl -sI -A 'StickyUA' $E/$CODE        # variant stays constant across requests
```
Verified: iPhone→App Store, Android→Play, Desktop→web fallback; GEO `IN`→india (via simulated
`x-vercel-ip-country`), no-country→fallback; A-B same-visitor **5/5** sticky, different visitors
distribute across A/B.

## Why (one line each)
QR encodes the short URL → the physical QR is immune to destination edits. Rules evaluated at resolve
time → one link serves per-device/geo/experiment without new links. **Sticky A-B** via a deterministic
hash of a stable visitor key → a valid experiment (a user never flip-flops variants). Ruled links are
non-cacheable → the cache can never serve one visitor's variant to another (same reason capped/expiring
links bypass the cache).

## Trade-offs / caveats
- **Geo** depends on an edge-provided country header (`CF-IPCountry` / `x-vercel-ip-country`) — `Unknown`
  with no real edge; verified via a simulated header. Real geo = the CDN's IP-country or a MaxMind DB.
- **A-B bucketKey = ip|ua** (cookieless) — good enough and stateless; a first-party cookie (`linkly_vid`)
  is the sturdier refinement (survives IP changes).
- Analytics still records the *code*, not the routed variant — per-variant conversion tracking is a
  follow-up.

## Decisions referenced
- [ADR-0010 smart routing](../../adr/0010-smart-routing.md)

---
**✅ Phase 3 complete** — branded domains, QR, and smart routing. Next: Phase 4 (teams/RBAC + API keys +
bulk + a flagship).
