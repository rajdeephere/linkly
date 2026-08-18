# Phase 04 — Teams/API/bulk + one flagship ⭐

**Status:** ✅ complete (Days 12–13) · **Roadmap:** Days 12–13

## Goal
Turn the tool into a **multi-tenant platform** (teams, API, bulk) and ship **one flagship surface** —
done well, not three half-built.

## Scope
**In:** workspaces + `Membership` RBAC (owner/admin/member); scoped **API keys** + public API; **bulk
CSV import** (async job, batch KGS allocation); **one flagship** — recommended **link-in-bio** (hosted
mini-page builder + themes, served at the edge). Alternatives: password-protected/cloaked links +
interstitials, or deep-link intelligence.
**Out:** deploy/observability (Phase 5), billing (stretch).

## Architecture delta
```
   management api: workspace scope + role checks on every mutation; api-key auth path; bulk import worker
   link-in-bio: BioPage (slug, theme, blocks[]) served from the edge
```
Data model ([../data-model.md](../data-model.md)); bulk uses [ADR-0002](../adr/0002-kgs-base62-codes.md)
batch allocation; abuse guard ([ADR-0009](../adr/0009-safe-browsing-abuse.md)).

## Done when
- [x] **Day 12:** JWT auth (register/login) + workspaces/`Membership` RBAC (workspace isolation verified);
      create links via a scoped **API key**; **bulk CSV import** with per-row error isolation.
      *(Invite flow + a queued 10k job are follow-ups; the auth+scoping+keys+bulk core is done.)*
- [x] **Day 13:** link-in-bio — workspace-scoped management + a public server-rendered `/bio/{slug}`
      page with themed blocks (verified: create, blocks, public JSON, hosted render, slug clash 409).
- [x] **Frontend catch-up:** the full authed web dashboard — login/register, a guarded nav, and pages for
      links (create/copy/QR/analytics/delete), custom domains (add→verify), API keys (shown once), and the
      bio editor. Closes the earlier "web login UI deferred" gap. Bio blocks became fully editable from the
      UI: `DELETE .../blocks/{blockId}` (deletes + compacts positions to `0..n-1`) and
      `PUT .../blocks/order` (permutation-guarded reorder). Also added `GET /v1/domains` (list).

**✅ Phase 4 complete** — multi-tenant platform (auth, RBAC, API keys, bulk) + the link-in-bio flagship,
now fully usable in the browser end-to-end.

## Maps to
- ADRs: [0002 KGS batch](../adr/0002-kgs-base62-codes.md), [0009 abuse](../adr/0009-safe-browsing-abuse.md)
- Hard scenarios: bulk import (#16), password-protected (#17)
