# Linkly — Data Model

Relational metadata in **PostgreSQL**; the append-only click fact table in **ClickHouse** (Phase 1;
Postgres-first for the MVP — [ADR-0007](./adr/0007-clickhouse-analytics.md)). Uniqueness on a short
code is **per-domain** (`(domain, code)`) once branded domains land — two tenants can both own `/launch`.

## Domain model

```
Workspace   (id, name, plan, createdAt)                         -- tenant boundary
User        (id, email, name, createdAt)
Membership  (workspaceId, userId, role[owner|admin|member])     -- RBAC (Phase 4)
Domain      (id, workspaceId, hostname, verified, tlsStatus)    -- branded domains (Phase 3)
Link        (id, workspaceId, domainId?, code, destinationUrl,
             title, expiresAt, expiresUrl?, clickLimit?, clickCount,
             passwordHash?, isCloaked, utm*, createdAt, updatedAt)
RoutingRule (id, linkId, type[geo|device|os|time|ab],
             condition, destinationUrl, weight, priority)        -- smart routing (Phase 3)
ApiKey      (id, workspaceId, hashedKey, scopes[])               -- Phase 4
BioPage     (id, workspaceId, slug, theme, blocks[])             -- link-in-bio (Phase 4)
ClickEvent  (linkId, workspaceId, ts, ipHash, country, region,
             city, device, os, browser, referrer, ua, isBot,
             ruleMatched)                                        -- ClickHouse, append-only (Phase 1)
```

## Day-1 schema (shipped — `V1__init.sql`)

```sql
CREATE TABLE workspace (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    plan VARCHAR(50) NOT NULL DEFAULT 'free',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_user (            -- 'user' is reserved in Postgres
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL UNIQUE,
    name VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspace (id),
    code VARCHAR(64) NOT NULL,
    destination_url VARCHAR(2048) NOT NULL,
    title VARCHAR(255),
    expires_at TIMESTAMPTZ,
    click_limit BIGINT,
    click_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_link_code ON link (code);   -- becomes (domain_id, code) in Phase 3
CREATE INDEX ix_link_workspace ON link (workspace_id);
```

## Planned migrations (by phase)
| Migration | Adds | Phase |
|-----------|------|-------|
| `V2` | `membership` + `api_key` (RBAC, keys) | 4 |
| `V3` | `domain` + `link.domain_id` (FK); switch unique index to `(domain_id, code)` | 3 |
| `V4` | `routing_rule` | 3 |
| `V5` | `bio_page` | 4 |
| ClickHouse DDL | `click_events` table (denormalized, `ORDER BY (link_id, ts)`) | 1 |

## Conventions
- **UUID PKs** (`gen_random_uuid()` DB-side, `GenerationType.UUID` app-side) — non-enumerable, no central
  sequence, multi-region-friendly.
- **Enums as `STRING`** (`@Enumerated(STRING)`) — stable across reordering.
- **`TIMESTAMPTZ`** everywhere, UTC.
- **Flyway owns the schema**; Hibernate runs `ddl-auto=validate` ([ADR-0012](./adr/0012-flyway-schema.md)).
- Click events are **immutable + denormalized** (workspace/link fields copied) so analytics needs no
  cross-store join ([ADR-0007](./adr/0007-clickhouse-analytics.md)).
