# ADR-0008 — Cache invalidation: write-through + explicit edge purge + short TTL

**Status:** Proposed (origin cache Day 5; edge purge Day 8) · **Date:** 2026-07-15

## Context
Resolve is cache-first (Redis at the origin, KV at the edge). The moment a link's destination is edited
or the link is deleted, a cached entry serving the old URL is a **correctness bug** and a support
nightmare. TTL-only expiry leaves a stale window as long as the TTL.

## Decision
On edit/delete: **write through** to Postgres, **delete** the origin Redis key, and **purge** the edge
KV key (fan-out to edge locations). Edge entries also carry a **short TTL** as a backstop.

## Consequences
- Edits are reflected on the next click, not TTL-seconds later.
- Purge fan-out is eventually consistent — a brief stale window remains across edge locations.
- A delete-vs-resolve race can serve a just-deleted link for milliseconds (bounded, acceptable).
- Reinforced by [ADR-0005](./0005-302-not-301.md): 302 guarantees future clicks re-resolve through us,
  so a purge actually takes effect (a 301 would defeat it).

## Alternatives
- **TTL-only**: simple, but stale windows are as long as the TTL.
- **No cache**: always correct, far too slow for the hot path.

## Revisit if
Purge propagation latency matters more → shorter TTL + versioned keys (`code@v2`) for instant cutover.
