# Linkly — Wire Protocol / API Contract

Two surfaces, deliberately separate ([ADR-0001](./adr/0001-resolver-separate-from-api.md)):
the **resolve path** (the hot redirect) and the **management API** (CRUD/admin).

---

## 1. Resolve path (the hot redirect)

```
GET /{code}
```
- **302** `Location: <destination>` on a hit (default; not 301 — [ADR-0005](./adr/0005-302-not-301.md)).
- **410 Gone** if expired or click-capped (or **302** to `expiresUrl` if configured).
- **404** if the code is unknown.
- **Password page** (interstitial) if the link is password-protected.
- Emits a **click event asynchronously** after responding — never on the response path
  ([ADR-0004](./adr/0004-analytics-off-hot-path.md)).
- Host-scoped once branded domains land: the `Host` header selects the domain, so lookup is
  `(domain, code)`.

**Latency budget:** <10ms at the edge for hot links; origin fallback only on miss / complex rule.

---

## 2. Management API (REST)

Auth: JWT (web session) or a scoped **API key** (`Authorization: Bearer <key>`). All routes are
workspace-scoped.

### Links
```
POST   /v1/links                 create { destinationUrl, alias?, domainId?, expiresAt?, clickLimit?, rules? }
GET    /v1/links                 list (filter by tag/domain/search)
GET    /v1/links/{id}            detail
PATCH  /v1/links/{id}            edit destination/expiry/rules   → purge cache (ADR-0008)
DELETE /v1/links/{id}            soft-delete                     → purge cache
POST   /v1/links/bulk            batch create / CSV import (async job)
GET    /v1/links/{id}/qr         QR image (png|svg)
GET    /v1/links/{id}/analytics  { clicks, timeseries, byCountry, byDevice, byReferrer }
```

### Domains (Phase 3)
```
POST   /v1/domains               add { hostname }  → returns verification record
GET    /v1/domains/{id}          { verified, tlsStatus }
POST   /v1/domains/{id}/verify   re-check DNS + trigger TLS
```

### Routing / keys / bio
```
POST   /v1/links/{id}/rules      add routing rule (geo|device|os|time|ab)
POST   /v1/api-keys              create scoped key
GET    /v1/bio/{slug}            link-in-bio page data
```

### Ops (shipped Day 1)
```
GET    /ping                     { app, status, seededLinks }   — smoke test (DB round-trip)
GET    /actuator/health          Spring Boot health (Postgres + Redis)
```

---

## Errors
Uniform `ApiError` envelope `{ timestamp, status, error, message, fieldErrors? }` via a global
`@RestControllerAdvice`. Notable mappings: **409** alias/code clash, **422** unsafe URL (Safe-Browsing),
**429** rate-limited, **400** validation (`fieldErrors`), **404** unknown link.

## Conventions
- Versioned under `/v1`. JSON in/out. UTC `TIMESTAMPTZ`.
- Idempotency: custom-alias create is first-writer-wins on the `(domain, code)` unique constraint.
- The resolve path shares **no runtime** with this API; it only shares the link-lookup module.
