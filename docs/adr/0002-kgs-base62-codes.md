# ADR-0002 — Short codes via a Key Generation Service (pre-allocated base62)

**Status:** Proposed (implement Phase 1 / Day 3) · **Date:** 2026-07-15

## Context
The signature "design a URL shortener" question: how do you turn a link into a short code that is
**unique**, **short**, **collision-free at scale**, and **not enumerable** (so nobody can walk
`a, b, c…` and scrape every link) — without a slow collision-retry loop on the write path?

## Decision
A **Key Generation Service (KGS)** hands out unique **base62** codes drawn from a **pre-generated /
range-allocated pool**. The write path just claims the next unused code (atomic pop) — no hashing, no
retry. Codes are made **non-sequential** by encoding a shuffled counter (or a Snowflake id through a
bijective scramble), so they're unique *and* unguessable.

## Consequences
- O(1) allocation; guaranteed uniqueness; no collision handling on the hot write.
- Non-sequential ⇒ not enumerable — defends against scraping.
- The KGS is a component to run + refill (low-watermark top-up); it must never re-issue a used key
  after a restart (pool state is durable in Redis/DB).

## Alternatives
- **Hash-and-truncate** (MD5/SHA → first N chars): collides as the table grows → retry loop on write.
- **Auto-increment → base62**: trivial to generate but **enumerable** (sequential) — a scraping hazard.
- **Random NanoID + unique constraint retry**: simplest; fine at low scale, but collision probability
  and retry cost climb with table size.

## Revisit if
Code space nears exhaustion → widen code length by one char (base62 gives 62× headroom per char).
