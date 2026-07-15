# Phase 04 — Teams/API/bulk + one flagship ⭐

**Status:** ⬜ · **Roadmap:** Days 12–13

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
- [ ] **Day 12:** invite a teammate with a role; create links via an API key; import 10k links via CSV.
- [ ] **Day 13:** the flagship (link-in-bio) works end-to-end.

## Maps to
- ADRs: [0002 KGS batch](../adr/0002-kgs-base62-codes.md), [0009 abuse](../adr/0009-safe-browsing-abuse.md)
- Hard scenarios: bulk import (#16), password-protected (#17)
