# Deployment runbooks (Run / Ship)

The hands-on **run/deploy** track — how to *run* Linkly locally and ship it. Detailed when it lands,
outlined before.

| # | Topic | Status |
|---|-------|--------|
| [01](./01-local-docker-dev.md) | Local docker dev — infra (Postgres/Redis/ClickHouse/Kafka) + api + web | ✅ full |
| 02 | Dockerize api + resolver + web (images) | ⬜ outlined |
| 03 | Cloud: ECS/EKS + edge (Cloudflare/Vercel) + CI/CD (GitHub Actions) + Terraform | ⬜ outlined |
| 04 | Observability: OpenTelemetry → Prometheus/Grafana | ⬜ outlined |

Companion: the **build** track in [`../implementation/`](../implementation/).
Design context: [`../../DEPLOYMENT-ARCHITECTURE.md`](../../DEPLOYMENT-ARCHITECTURE.md).
