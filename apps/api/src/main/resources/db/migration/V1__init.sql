-- Linkly — initial schema (Day 1).
-- Metadata only; click events live in ClickHouse later (see docs/ARCHITECTURE.md, ADR-7).

CREATE TABLE workspace (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    plan       VARCHAR(50)  NOT NULL DEFAULT 'free',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email      VARCHAR(320) NOT NULL UNIQUE,
    name       VARCHAR(255),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE link (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id    UUID          NOT NULL REFERENCES workspace (id),
    code            VARCHAR(64)   NOT NULL,
    destination_url VARCHAR(2048) NOT NULL,
    title           VARCHAR(255),
    expires_at      TIMESTAMPTZ,
    click_limit     BIGINT,
    click_count     BIGINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Uniqueness is per-domain once branded domains land; global for now.
CREATE UNIQUE INDEX ux_link_code ON link (code);
CREATE INDEX ix_link_workspace ON link (workspace_id);

-- Seed data so `/ping` returns a non-zero count and there's a link to resolve on Day 2.
INSERT INTO workspace (id, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'Demo Workspace');

INSERT INTO link (workspace_id, code, destination_url, title)
VALUES ('00000000-0000-0000-0000-000000000001', 'launch',
        'https://github.com/rajdeephere/linkly', 'Linkly repo');
