# Phase 03 — Branded domains + QR + smart routing

**Status:** ⬜ · **Roadmap:** Days 10–11

## Goal
Ship the top marketable features: **branded custom domains** over HTTPS, **dynamic QR codes**, and
**smart routing** (geo/device/OS/time + A-B split).

## Scope
**In:** add-domain flow → DNS TXT/CNAME verification → on-demand per-host TLS → resolver routes by
`Host`; dynamic QR per link (stays valid on edit) → S3; routing rules + A-B split with sticky bucketing
(deep-link to app stores); 301 opt-in per link.
**Out:** teams/RBAC, bulk API (Phase 4), link-in-bio (Phase 4).

## Architecture delta
```
   add domain ─► verify DNS ─► auto-TLS ─► resolver routes by Host (uniqueness = (domain, code))
   resolve ─► evaluate geo/device/time/AB rules inline ─► destination
```
Custom domains ([ADR-0006](../adr/0006-custom-domains-tls.md)); routing
([ADR-0010](../adr/0010-smart-routing.md)).

## Done when
- [ ] **Day 10:** point `go.acme.com` at Linkly → branded short links over HTTPS; QR generated + downloadable.
- [ ] **Day 11:** one link sends iOS→App Store, Android→Play, else web; A-B returning-visitor stays on
      the same variant; a QR survives a destination edit.

## Maps to
- ADRs: [0006 custom domains + TLS](../adr/0006-custom-domains-tls.md), [0010 smart routing](../adr/0010-smart-routing.md)
- Hard scenarios: new domain TLS (#8), geo-target (#12), A-B sticky (#13), deep link (#15)
