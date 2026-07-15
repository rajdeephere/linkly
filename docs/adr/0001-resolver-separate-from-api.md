# ADR-0001 — Redirect resolver is a separate service from the management API

**Status:** Proposed (implement Phase 2 / Day 8) · **Date:** 2026-07-15

## Context
Redirects and management (CRUD/auth/dashboard) have opposite profiles. Resolving `GET /{code}` is a
read that outnumbers writes 100:1+, must return in single-digit milliseconds, and spikes hard when a
link goes viral. The management API is write/admin traffic with auth, an ORM, and heavier endpoints.
Sharing one process means a viral link can exhaust the same connection pool and CPU the dashboard needs.

## Decision
Make the **resolve/redirect path its own deployable** (edge + a minimal Spring Boot origin), physically
separate from the management API. The resolver does one thing: code → destination → 302 + async event.

## Consequences
- The hot path scales horizontally on its own, independent of admin traffic.
- A traffic spike on redirects can't starve the dashboard (and vice-versa).
- Two deployables to run; the shared link-lookup logic must be factored into a common module.

## Alternatives
- **One monolith** — simplest; fine at low traffic (and correct for the MVP). Rejected at scale because
  the hot path and admin path contend for the same resources — the classic mistake.
- **Serverless-only resolver** — see [ADR-0003](./0003-hybrid-edge-origin.md).

## Revisit if
Traffic stays tiny — then keep it a monolith and split later. The split is a scale decision, not a
day-one one; the Day-1 scaffold is deliberately a single `apps/api`.
