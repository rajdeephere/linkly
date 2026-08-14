# Impl 12 — Day 12: Auth, teams/RBAC, API keys, bulk import

**Outcome:** the api is now multi-tenant — JWT auth, workspace-scoped RBAC, scoped API keys, and bulk
CSV import. **Status:** ✅ shipped & verified. *(Starts Phase 4.)*

## Part A — Auth + teams/RBAC
1. **Deps/migration:** `spring-boot-starter-security` + `jjwt`; `V6` adds `app_user.password_hash` and
   the `membership` table (workspace ↔ user, role owner|admin|member).
2. **Auth** (`auth/`): `JwtService` (HS256 issue/verify, claims: sub=userId, wsid, role, email),
   `JwtProperties`, `JwtAuthenticationFilter`, `AppUserPrincipal`, `SecurityConfig` (stateless, BCrypt,
   CSRF off, public = register/login/ping/actuator, 401 entry point), `AuthService`/`AuthController`
   (`register` → user + workspace + owner membership → JWT; `login`; `/me`).
3. **RBAC scoping:** every management endpoint now resolves within the caller's workspace —
   `LinkRepository.findByIdAndWorkspaceId` / `findByWorkspaceId…`; links, domains, rules, analytics, and
   QR all go through the scoped lookup (cross-workspace access 404s). `DELETE` requires owner/admin.
   Added `GET /v1/links` (scoped list).

## Part B — API keys + bulk import
4. **API keys** (`apikey/`): `V7` `api_key` table (stores only the SHA-256 hash + a display prefix).
   `ApiKeyService` mints `lk_<40 base62>` (plaintext returned **once**); `ApiKeyAuthenticationFilter`
   runs **before** the JWT filter — a `Bearer lk_…` resolves to a workspace principal (the JWT filter
   skips if already authenticated). `ApiKeyController` create (owner/admin) + list.
5. **Bulk import:** `POST /v1/links/bulk` (CSV body) → `LinkService.createBulk` parses
   `destinationUrl[,alias][,title]` rows, creates each **independently** (per-row error isolation, not
   `@Transactional`), capped at 1000, returns `{requested, created, failed[]}`. Works with an API key.

## Verify
```bash
# auth + isolation
curl -s -X POST $A/v1/auth/register -d '{"email":"a@x.com","name":"A","password":"password123"}' -H 'Content-Type: application/json'  # 201 + JWT
curl -s $A/v1/links -H "Authorization: Bearer $JWT"     # scoped list ; no token → 401
# API key + bulk
curl -s -X POST $A/v1/api-keys -H "Authorization: Bearer $JWT" -d '{"name":"ci"}' -H 'Content-Type: application/json'  # key shown once
printf 'https://ex.com/a,promoA\nnot-a-url,bad\n' | curl -s -X POST $A/v1/links/bulk -H "Authorization: Bearer lk_…" -H 'Content-Type: text/csv' --data-binary @-
```
Verified: no-token→401; A/B **workspace isolation** (B can't see/GET A's links → 404); login/wrong-pw/
dup-email → 200/401/409; owner delete → 204; API key creates a link + a bad key → 401; **bulk** creates
valid rows and reports per-row failures (dup alias, bad URL).

## Why (one line each)
Stateless JWT → any api instance authenticates any request (scale-out). Register-makes-a-workspace →
every user is a tenant from day one. Workspace-scoped repository lookups → isolation is enforced in one
place, not per-controller `if`s. API keys store only a hash (leak-safe) + shown once. API-key filter
before JWT (+ JWT skips-if-authenticated) → one `Authorization` header cleanly supports both.
Per-row-isolated bulk → one bad row never fails the batch.

## War-story (caught in verification)
Bulk import **bypassed the DTO's `@Pattern` validation** (it builds `CreateLinkRequest` directly, not via
`@Valid`), so `not-a-url` got created as a link's destination. Caught it in the verify run (created=3
when it should've been 2). Fixed by moving the http(s) guard into `LinkService.create` itself —
**defense-in-depth: validate at the service boundary, not only at the controller** — since bulk (and any
future caller) skips controller validation. (See `linkly-docs` war-story #15.)

## Caveats / follow-ups
- **Web login UI deferred** — the api now requires auth, so the web Shortener 401s until a login screen
  is added (a frontend task, not on the Phase-4 API critical path).
- **Shared default domain** → aliases on it are globally unique across workspaces (correct — like a
  shared short host); custom domains give per-tenant namespaces.
- Bulk is synchronous + capped; a queued job with progress is the scale step. API keys have a single
  `role`, not fine-grained scopes yet. Member-can't-delete (403) is code-verified (no invite flow yet).

## Decisions referenced
- [ADR-0010 (routing)](../../adr/0010-smart-routing.md) · auth/RBAC follow the spec's teams model.

## Next
Day 13 — one flagship (recommended: link-in-bio). Then Phase 5 (deploy + harden).
