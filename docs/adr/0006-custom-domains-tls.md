# ADR-0006 — Multi-tenant custom domains + on-demand per-host TLS

**Status:** Proposed (implement Phase 3 / Day 10) · **Date:** 2026-07-15

## Context
Branded domains (`go.acme.com/launch`) are the #1 paid feature in this category. Serving them means
routing by `Host`, issuing TLS certs per hostname, and doing it without letting anyone mint a cert for
a domain they don't own.

## Decision
A tenant adds a hostname → proves ownership via a **DNS TXT/CNAME record** → TLS is provisioned
**automatically per-hostname** (Caddy on-demand TLS or Cloudflare for SaaS). The resolver scopes the
code lookup by the `Host` header, so uniqueness is **`(domain, code)`** — two tenants can both own
`/launch`.

## Consequences
- True branded short links over HTTPS, self-service.
- Cert issuance must be **gated behind verification** (else it's a cert-mining attack surface).
- DNS-propagation UX is fiddly (clear "pending / verified / active" states needed).

## Alternatives
- **Wildcard cert on our own subdomains only**: no real branding.
- **Manual cert upload**: terrible UX, no automation.

## Revisit if
Cert volume grows large → move to dedicated ACME infrastructure with rate-limit management.
