# ADR-0010 — Smart routing at resolve time; A-B with sticky bucketing

**Status:** Proposed (implement Phase 3 / Day 11) · **Date:** 2026-07-15

## Context
A single link may need to send visitors to different destinations by **geo / device / OS / time**, or
**split-test** (A-B) between destinations. Whatever the rule, it must run on the hot path with near-zero
added latency, and an A-B test must be statistically valid.

## Decision
Rules are evaluated **at resolve time** — at the edge when the needed context (geo, UA) is available
there, else at the origin. **A-B splits bucket a visitor deterministically**: hash a stable visitor
identifier (cookie/IP) → a fixed variant, so a returning visitor always lands on the same destination.

## Consequences
- Targeting/splitting adds negligible latency (evaluated inline, no extra hop).
- Sticky bucketing keeps A-B experiments uncorrupted.
- Rule-eval logic is duplicated edge + origin (the [ADR-0003](./0003-hybrid-edge-origin.md) tension);
  geo accuracy depends on the edge provider.

## Alternatives
- **Redirect to a rules micro-service**: clean separation, but adds a network hop — too slow for the
  hot path.

## Revisit if
Rules grow complex (multi-condition, weighted trees) → a compiled rule representation cached at the edge.
