# Phase 05 — Deploy + harden + productize

**Status:** ⬜ · **Roadmap:** Days 14–15

## Goal
Get Linkly **live, observable, hardened, and documented** — a public URL plus the war-stories that make
it interview-gold.

## Scope
**In:** dockerize `api`/`resolver`; deploy to ECS/EKS behind an ALB; edge on Cloudflare/Vercel; web on
Vercel; GitHub Actions CI/CD; Terraform infra; OpenTelemetry tracing a resolve end-to-end (edge → miss →
origin → Redis → Postgres → event); Grafana dashboard; rate limits, abuse guard, GDPR retention;
security pass (open-redirect checks, auth); README polish + STAR war-stories.
**Out (stretch):** multi-region edge, billing/plans, usage limits.

## Architecture delta
```
   Cloudflare/Vercel edge ─miss─► ALB ─► ECS/EKS {api, resolver, consumers} ─► RDS · ElastiCache · MSK · ClickHouse
   OTel → Prometheus/Grafana ; GitHub Actions build→test→deploy ; Terraform IaC
```
Deploy strategy ([../DEPLOYMENT-ARCHITECTURE.md](../DEPLOYMENT-ARCHITECTURE.md)).

## Done when
- [ ] **Day 14:** live public URL; a trace of one resolve crossing edge → origin in Grafana.
- [ ] **Day 15:** load-tested; security pass done; war-stories written (hybrid resolve, enumeration
      defense, cache-invalidation drill).

## Maps to
- Docs: [../DEPLOYMENT-ARCHITECTURE.md](../DEPLOYMENT-ARCHITECTURE.md)
- Deployment runbooks: [`../step-by-step-implementation/deployment/`](../step-by-step-implementation/deployment/)
