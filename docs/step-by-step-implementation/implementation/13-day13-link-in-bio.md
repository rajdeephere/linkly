# Impl 13 — Day 13: Link-in-bio (the flagship)

**Outcome:** a Linktree-style hosted page per workspace — managed behind auth, served publicly at
`/bio/{slug}` and rendered by the web app. **Status:** ✅ shipped & verified. **This completes Phase 4.**

## Build order (`apps/api`)
1. **Migration** `V8__bio_pages.sql` — `bio_page` (workspace-owned, globally-unique `slug`, title,
   avatar, bio, theme) + `bio_block` (label, url, position; `ON DELETE CASCADE`).
2. **Domain** `BioPage` / `BioBlock` entities + repos (`findBySlug`, `findByIdAndWorkspaceId`,
   `findByBioPageIdOrderByPositionAsc`).
3. **Service** `BioService` — create (slug unique → 409), list/get/update (workspace-scoped), addBlock
   (auto-appends position), and **`publicBySlug`** (no scoping — the visitor-facing read).
4. **Controllers** — `BioController` (`/v1/bio`, auth'd, workspace-scoped) +
   `PublicBioController` (`GET /bio/{slug}`, public). `SecurityConfig` permits `/bio/**`.

## Build order (`apps/web`)
5. `app/bio/[slug]/page.tsx` — a **server-rendered** (SEO-friendly) public page: fetches
   `${API_URL}/bio/{slug}`, renders avatar/title/bio + blocks as full-width buttons, themed
   (`default` / `dark` / `sunset`). Unknown slug → a friendly "doesn't exist".

## Why it fits the engine
Bio blocks are just URLs — point them at **Linkly short links** and every bio click flows through the
same resolve → analytics → routing pipeline. The bio page is a *second public surface on one engine*,
not a separate product. It's workspace-owned (RBAC from Day 12) but publicly readable by slug (like the
shared short host — slugs are a global namespace).

## Verify
```bash
# manage (auth'd)
curl -s -X POST $A/v1/bio -H "Authorization: Bearer $JWT" -H 'Content-Type: application/json' \
  -d '{"slug":"nova","title":"Nova Studio","bio":"designer","theme":"sunset"}'
curl -s -X POST $A/v1/bio/$ID/blocks -H "Authorization: Bearer $JWT" -H 'Content-Type: application/json' \
  -d '{"label":"Portfolio","url":"https://nova.example.com"}'
# public
curl -s $A/bio/nova                 # JSON, no auth
curl -s http://localhost:3000/bio/nova   # server-rendered hosted page
```
Verified: create + 2 blocks; public JSON (no auth); slug clash → 409; hosted page renders title + blocks +
theme; workspace-scoped management (another workspace can't GET the page by id, but the public `/bio/slug`
is open to all).

## Trade-offs / follow-ups
- **Soft-404:** an unknown slug renders a "doesn't exist" page with HTTP 200; a real 404 status is a
  small SEO polish.
- Block **reorder/delete** endpoints and richer block types (headers, embeds) are natural extensions.
- **Web login UI still deferred** — bio *management* needs a JWT (verified via curl); the public page is
  open, so the visitor-facing flagship is fully demoable in a browser.

## Decisions referenced
- Data model ([../../data-model.md](../../data-model.md)); RBAC scoping (Day 12).

---
**✅ Phase 4 complete** — teams/RBAC, API keys, bulk import, and the link-in-bio flagship. Only Phase 5
(deploy + harden) remains.
