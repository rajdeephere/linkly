# Impl 05 — Day 5: Edit/delete + Redis cache-aside + invalidation

**Outcome:** resolve is cache-aside (Redis); `PATCH`/`DELETE` **write-through + purge** so an edit is
reflected on the very next click (never stale) and a delete → 404. **Status:** ✅ shipped & verified.
**This completes Phase 0.**

## Prerequisites
- Day 4 ([04-day4-aliases-expiry-safety](./04-day4-aliases-expiry-safety.md)); infra up incl. Redis.

## Build order (backend — `apps/api`)
1. **Cache** `link/LinkCache` — `link:{code} → destinationUrl` in Redis; `getDestination` / `put`
   (1h TTL backstop) / `evict`.
2. **Resolve, cache-aside** `LinkService.resolve`:
   - Redis hit → 302 immediately (no DB).
   - miss → load; time-expired → fallback/410; **capped** → atomic increment (never cached);
     **time-limited** → DB every time (never cached); **plain** (no cap, no expiry) → `cache.put` + 302.
   - *Only plain links are cached* — capped/expiring links carry per-request state, so caching them
     would break the cap / expiry. (ADR-0008.)
3. **Edit** `LinkService.update(id, UpdateLinkRequest)` — partial update (null = unchanged);
   re-screens a changed destination (422); `save` then **`cache.evict(code)`**.
4. **Delete** `LinkService.delete(id)` — delete row then `cache.evict(code)`.
5. **DTO** `UpdateLinkRequest` — all-optional, validated only when present (code/alias immutable).
6. **Endpoints** `LinkController` — `PATCH /v1/links/{id}` (200 + `LinkResponse`),
   `DELETE /v1/links/{id}` (204).

*(No frontend change this day — management is via the API; a link list/editor lands with the dashboard,
Day 7.)*

## Verify
```bash
B=http://localhost:8081; J='-H Content-Type:application/json'
ID/CODE from: curl -s -X POST $B/v1/links $J -d '{"destinationUrl":"https://anthropic.com/"}'
curl -sI $B/$CODE | grep -i location                 # → anthropic (warms cache)
docker exec linkly-redis redis-cli get link:$CODE     # → https://anthropic.com/
curl -s -X PATCH $B/v1/links/$ID $J -d '{"destinationUrl":"https://claude.com/NEW"}'  # 200
docker exec linkly-redis redis-cli get link:$CODE     # → (empty — purged)
curl -sI $B/$CODE | grep -i location                 # → https://claude.com/NEW  (not stale!)
curl -s -o /dev/null -w '%{http_code}' -X DELETE $B/v1/links/$ID   # 204
curl -s -o /dev/null -w '%{http_code}' $B/$CODE                    # 404
```
Verified: resolve warms `link:{code}`; PATCH purges it; next resolve serves the **new** URL and
re-caches; DELETE purges + resolve 404; PATCH to a `phishing` URL → 422.

## Why (one line each)
Cache-aside on the hot path → most resolves skip the DB entirely. Explicit **evict on write** (not just
TTL) → an edit is correct immediately, not "eventually" (ADR-0008). Only-cache-plain-links → the cache
can never be wrong about a cap/expiry it doesn't know changed. 302-not-301 (Day 2) is what *lets* the
purge take effect — a 301 would be cached in the browser past our reach.

## Trade-offs noted
- Partial `PATCH` applies only **non-null** fields, so it can't null-out an existing expiry/limit — a
  known PATCH limitation; a later pass can add explicit clears.
- TTL is a backstop, not the mechanism; at the edge (Phase 2) the purge fans out to edge locations
  (eventually consistent — a brief stale window), which is the ADR-0008 story at scale.

## Decisions referenced
- [ADR-0008 cache invalidation](../../adr/0008-cache-invalidation.md) ·
  [ADR-0005 302-not-301](../../adr/0005-302-not-301.md)

---
**✅ Phase 0 complete** — a correct, safe, fast, editable shortener on one service. Next arc: Phase 1,
the analytics pipeline (Day 6).
