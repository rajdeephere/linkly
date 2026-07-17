# Impl 02 — Day 2: Shorten + 302 resolve + shorten bar

**Outcome:** `POST /v1/links` mints a short code for a destination; `GET /{code}` **302**-redirects to it;
the Next.js home page is a shorten bar with one-click copy. Paste a URL → get a short link → click it →
land on the destination. **Status:** ✅ shipped & verified end-to-end.

## Prerequisites
- Day 1 scaffold ([01-day1-scaffold](./01-day1-scaffold.md)); infra up
  ([deployment/01-local-docker-dev](../deployment/01-local-docker-dev.md)).

## Build order (backend — `apps/api`)
1. **Code generation** `link/CodeGenerator` — random **base62**, length 7. *Temporary:* uniqueness comes
   from the `(code)` unique index + a retry loop; the proper KGS (pre-allocated pool, no write-path
   retry) lands Day 3 ([ADR-0002](../../adr/0002-kgs-base62-codes.md)).
2. **Service** `link/LinkService` — `create(destinationUrl, title)` generates a code, checks
   `existsByCode`, inserts under the seeded default workspace, and **retries on a
   `DataIntegrityViolationException`** (the concurrent-insert race); `resolve(code)` = `findByCode`;
   `findById(id)` tolerates a malformed UUID (→ empty, not a 500).
3. **DTOs** `link/dto` — `CreateLinkRequest { destinationUrl (@NotBlank + http(s) @Pattern), title }`,
   `LinkResponse { id, code, shortUrl, destinationUrl, title, createdAt }` (explicit projection via
   `LinkResponse.from(link, baseUrl)` — never the entity).
4. **Management API** `link/LinkController` — `POST /v1/links` → **201** `LinkResponse`;
   `GET /v1/links/{id}` → detail or 404. `shortUrl` built from `linkly.base-url`.
5. **Redirect** `link/RedirectController` — `GET /{code:[0-9A-Za-z]+}` → **302** with `Location`, or 404.
   The base62 regex means `/favicon.ico` and `/actuator/**` never match; the literal `/ping` out-ranks
   the pattern. → [ADR-0005](../../adr/0005-302-not-301.md).
6. **Config** `config/LinklyProperties` (`linkly.base-url`) + `@EnableConfigurationProperties`;
   `application.yml` gains `linkly.base-url: ${LINKLY_BASE_URL:http://localhost:8081}`.
7. **Errors** `common/GlobalExceptionHandler` + `ApiError` — validation → **400** with `fieldErrors`;
   `ResponseStatusException` → its status. Uniform envelope.

## Build order (frontend — `apps/web`)
1. `components/shortener.tsx` — **client** component: URL input + Shorten button → `POST /api/v1/links`
   (proxied to `:8081`), renders the short link with a **Copy** button (`navigator.clipboard`); shows
   the API's `fieldErrors.destinationUrl` on 400.
2. `app/page.tsx` — server component: hero + `<Shortener/>` + a live status line (`getApiStatus`).

## Verify
```bash
BASE=http://localhost:8081
# create
curl -s -X POST $BASE/v1/links -H "Content-Type: application/json" \
  -d '{"destinationUrl":"https://anthropic.com/","title":"Anthropic"}'          # 201 + code + shortUrl
# resolve
curl -s -o /dev/null -D - $BASE/<code> | grep -iE "^HTTP|^location"             # 302 + Location
curl -s -o /dev/null -w "%{http_code}\n" $BASE/zzzzzzz                          # 404 unknown
curl -s -X POST $BASE/v1/links -H "Content-Type: application/json" \
  -d '{"destinationUrl":"not-a-url"}'                                           # 400 + fieldErrors
# through the web proxy
curl -s -X POST http://localhost:3000/api/v1/links -H "Content-Type: application/json" \
  -d '{"destinationUrl":"https://github.com/rajdeephere/linkly"}'              # 201 (proxied → :8081)
```
Verified: 201 create · `302 Location` resolve · 404 unknown · 400 bad-URL (`fieldErrors`) · `/ping`
still routes · web proxy create + returned link resolves.

## Why (one line each)
302 not 301 → clicks keep flowing through us (analytics + editability)
([ADR-0005](../../adr/0005-302-not-301.md)). Base62-regex path var → the redirect route can't swallow
`/ping`, `/actuator`, or `favicon.ico`. Unique index + retry → correctness even under a concurrent
same-code race. DTO projection → the entity never leaks onto the wire. Client component only for the
interactive bar → the page stays a server component.

## Decisions referenced
- [ADR-0005 302 redirect](../../adr/0005-302-not-301.md) ·
  [ADR-0002 KGS codes](../../adr/0002-kgs-base62-codes.md) (Day-2 generator is the interim version)

## Next
Day 3 replaces `CodeGenerator` with the **KGS** (pre-allocated, non-enumerable pool) and proves an
enumeration script can't scrape the link set.
