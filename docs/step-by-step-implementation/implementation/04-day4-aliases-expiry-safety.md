# Impl 04 — Day 4: Aliases, expiry, safety, rate limits

**Outcome:** custom aliases (409 on clash), expiry by date **and** click cap (→ 410 or a fallback
redirect), Safe-Browsing screening on create (422), and per-IP rate limiting (429). **Status:** ✅
shipped & verified.

## Prerequisites
- Day 3 ([03-day3-kgs](./03-day3-kgs.md)); infra up incl. Redis (rate limiter uses it).

## Build order (backend — `apps/api`)
1. **Migration** `V2__link_expires_url.sql` — add `expires_url` (a fallback destination for
   expired/capped links; `expires_at`/`click_limit`/`click_count` already exist from V1). `Link` gains
   the `expiresUrl` field.
2. **DTO** `CreateLinkRequest` — add optional `alias` (`@Pattern ^[0-9A-Za-z]{1,64}$`), `expiresAt`
   (`@Future`), `clickLimit` (`@Positive`), `expiresUrl` (http(s)). `hasAlias()` treats blank as "none".
3. **Safety** `UrlSafetyChecker` interface + `StubUrlSafetyChecker` (blocks a local marker list:
   `malware`, `phishing`, Google's `testsafebrowsing.appspot.com`) — offline-provable stand-in for
   Google Safe Browsing; the real checker is a config-flag drop-in (ADR-0009).
4. **Rate limit** `common/RateLimiter` — Redis fixed window (`INCR` + first-hit `EXPIRE`); `allow(key,
   limit, window)`.
5. **Service** `LinkService.create(CreateLinkRequest)` — screen destination (→ **422**); if alias:
   `existsByCode` → **409** (unique index backstops the race → also 409); else `kgs.claim()`. Persist
   expiry/limit/fallback.
6. **Resolve** `LinkService.resolve` now returns a `ResolveOutcome {REDIRECT|GONE|NOT_FOUND, url}`:
   time-expired → fallback-or-410; else **atomic** `tryIncrementClick` (`UPDATE … WHERE click_limit IS
   NULL OR click_count < click_limit`) — 0 rows ⇒ capped ⇒ fallback-or-410; else 302.
7. **Controllers** — `LinkController.create` reads client IP (`X-Forwarded-For` → first hop, else
   `getRemoteAddr`), rate-limits (**429**), then creates. `RedirectController` switches on the outcome
   (302 / 410 / 404).
8. **Config** `LinklyProperties` gains `rateLimit.createPerMinute` (20) + `safeBrowsing.enabled`
   (false); `application.yml` wires them (env-overridable).

## Build order (frontend — `apps/web`)
- `components/shortener.tsx` — an **Options** expander adds custom alias (with a **live preview**
  `localhost:8081/{alias}`), click limit, and an `expires-at` picker (`datetime-local` → ISO via
  `toISOString()`). Optional fields are omitted when blank; API error messages (409/422/429) surface
  inline.

## Verify
```bash
B=http://localhost:8081
# alias + clash
curl -s -X POST $B/v1/links -d '{"destinationUrl":"https://anthropic.com/","alias":"mylaunch"}' -H 'Content-Type: application/json'   # 201
curl -s -X POST $B/v1/links -d '{"destinationUrl":"https://x.com/","alias":"mylaunch"}' -H 'Content-Type: application/json'          # 409
# safety
curl -s -X POST $B/v1/links -d '{"destinationUrl":"https://malware.example.com/"}' -H 'Content-Type: application/json'               # 422
# click cap → 302,302,410 ; expiresUrl fallback → dest then fallback ; time expiry → 302 then 410
# rate limit: burst > 20/min from one IP → 429s
```
Verified: alias 302 + clash 409 · malware 422 · clickLimit=2 → 302,302,**410** · expiresUrl → dest then
**fallback** · expiresAt now+3s → 302 then **410** · burst of 30 → ~20×201 then **429**.

## Why (one line each)
Atomic check-and-increment (`tryIncrementClick`) → **race-free** click cap under concurrent resolves
(no read-modify-write gap). `existsByCode` + unique index → friendly 409 *and* correctness under the
alias race. Safety screen on **create** (not the hot path) → abuse blocked cheaply (ADR-0009). Redis
fixed-window limiter → throttle abuse without per-request DB load. Stub safety checker → the rejection
path is provable with no API key.

## Trade-off noted
Resolve now does a small **write** (the click increment) on the hot path — acceptable for Day 4; Day 6
moves counting to the async analytics pipeline (ADR-0004), and a Redis counter can front the cap.

## Decisions referenced
- [ADR-0009 Safe Browsing + rate limits](../../adr/0009-safe-browsing-abuse.md) ·
  [ADR-0005 302/410 semantics](../../adr/0005-302-not-301.md)
