-- Phase 1 (Day 6): click analytics fact table.
-- Denormalized (stores link_code, not a FK) and append-only — the ClickHouse split (ADR-0007) keeps
-- the same shape. IP is stored hashed (GDPR), never raw.
CREATE TABLE click_event (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    link_code  VARCHAR(64)  NOT NULL,
    ts         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ip_hash    VARCHAR(64),
    country    VARCHAR(8),
    device     VARCHAR(32),
    os         VARCHAR(32),
    browser    VARCHAR(64),
    referer    VARCHAR(2048),
    is_bot     BOOLEAN      NOT NULL DEFAULT false
);

CREATE INDEX ix_click_event_code_ts ON click_event (link_code, ts);
