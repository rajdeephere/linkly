# ADR-0004 — Analytics off the hot path: fire-and-forget → stream → OLAP

**Status:** Proposed (implement Phase 1 / Day 6) · **Date:** 2026-07-15

## Context
Every resolve should record a click (geo, device, referrer). But the redirect must stay at pure
cache-lookup speed, and it must not fail or slow down if the analytics store is degraded.

## Decision
The resolver emits a click event to a **stream (Kafka/Kinesis)** **after** issuing the redirect and
**never waits** for it. A separate consumer enriches (geo/device/bot), hashes the IP, and **batch-inserts**
into the analytics store. The redirect path does zero synchronous analytics work.

## Consequences
- Redirect latency and availability are decoupled from analytics — an analytics outage can't take down
  redirects.
- Analytics are **eventually consistent** (seconds of lag) — the dashboard says "near real-time".
- At-least-once event delivery ⇒ occasional duplicates → dedup on `(linkId, ts, ipHash)`.

## Alternatives
- **Increment a Redis counter inline**: fast but loses rich per-click dimensions.
- **Synchronous DB write on resolve**: couples the hot path to the analytics store — never.

## Revisit if
Volume is tiny → a simpler async path (or even a batched Postgres insert) suffices; the stream is the
scale story. See [ADR-0007](./0007-clickhouse-analytics.md) for the store choice.
