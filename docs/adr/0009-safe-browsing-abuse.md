# ADR-0009 — Safe-Browsing scan on create + aggressive rate limits

**Status:** Proposed (implement Phase 0 / Day 4) · **Date:** 2026-07-15

## Context
URL shorteners are a **phishing/malware magnet** — they hide the real destination behind an opaque
code. Ship without abuse defense and the domain gets blocklisted by browsers/mail providers, and the
product dies.

## Decision
Every submitted destination is checked against **Google Safe Browsing** (plus scheme/format validation)
**before** a code is issued. Per-IP / per-key **rate limits** throttle bulk abuse. Flagged links get an
interstitial warning page.

## Consequences
- Bad URLs are rejected up front; the domain's reputation is protected.
- An external dependency + a little latency on **create** — acceptable, because create is *not* the hot
  path (resolve is).
- False positives need an appeal path.

## Alternatives
- **Async scan-after-create + takedown**: faster create, but briefly serves bad links.
- **No scanning**: irresponsible and ultimately fatal to deliverability.

## Revisit if
Volume warrants → add content-category classification and a reputation service beyond Safe Browsing.
