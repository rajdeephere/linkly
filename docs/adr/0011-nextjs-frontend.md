# ADR-0011 — Next.js App Router frontend, decoupled from the resolver

**Status:** Accepted (Day 1) · **Date:** 2026-07-15

## Context
The dashboard/marketing/link-in-bio surface needs a modern SaaS aesthetic (the bar Dub.co set) and must
not share any runtime with the latency-critical resolve path.

## Decision
Build the web app with **Next.js (App Router) + React + Tailwind + shadcn/ui**, talking to the Spring
Boot management API over REST. It shares **zero** runtime with the resolver. Deploy on Vercel (which
also hosts link-in-bio pages cheaply at the edge).

## Consequences
- Modern, fast dashboards (server components); the aesthetic target is reachable.
- Demonstrates full-stack range beyond the Java backend.
- A second ecosystem (TypeScript) to maintain; auth will span two systems (web session ↔ API JWT).

## Alternatives
- **Angular** (reuse the LinkUp skill set, consistent enterprise story): rejected here because the
  aesthetic target and edge-hosted link-in-bio fit React/Vercel better.

## As built (Day 1)
`apps/web` scaffolded: App Router, Tailwind + shadcn config, a landing page that live-checks the API via
a **server component** calling `GET /ping`. Dev proxies `/api/*` → `http://localhost:8081`. Production
build verified (`next build` clean; `/` is dynamically rendered because the status check is `no-store`).

## Revisit if
Link-in-bio or marketing needs diverge enough to warrant a separate app — split then, not now.
