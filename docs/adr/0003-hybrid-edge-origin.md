# ADR-0003 — Hybrid edge KV + Spring Boot origin for resolve

**Status:** Proposed (implement Phase 2 / Day 8) · **Date:** 2026-07-15

## Context
"Future-proof" means a redirect must resolve in <10ms from anywhere on earth and absorb viral spikes.
A single origin region adds a cross-continent hop for far-away visitors; going fully serverless at the
edge pulls the whole hot path off the JVM (and off the enterprise-backend story this project is built to
tell).

## Decision
**Hybrid.** Hot links live in a **global edge KV**; the edge worker resolves + redirects + emits the
click event, hitting the **Spring Boot origin** only on a miss (cold link, complex routing rule,
password / expiry check). The origin is the source of truth and warms the edge on miss.

## Consequences
- <10ms global resolve for the 99% hot links; origin protected behind the edge cache.
- You can defend **both** the modern-edge story and the enterprise-JVM story — the strongest resume
  narrative and genuinely the most future-proof.
- Two resolve implementations (edge + origin) must be kept in sync — a real maintenance cost.
- Edge/origin cache coherence must be handled on edit — see [ADR-0008](./0008-cache-invalidation.md).

## Alternatives
- **CloudFront in front of Spring only**: all in-wheelhouse, simpler; higher tail latency for cold
  regions, no edge compute for rule eval.
- **Full edge (Dub-style)**: fastest, most modern; loses the JVM origin story and vendor-locks the
  hot path.

## Revisit if
Keeping two resolvers in sync becomes a bug source → collapse toward one (likely edge-first) model.
