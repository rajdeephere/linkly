-- Phase 3 (Day 11): smart routing. Rules are evaluated at resolve time (ADR-0010): device/OS deep
-- links, geo targeting, and weighted A-B splits.
CREATE TABLE routing_rule (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    link_id         UUID          NOT NULL REFERENCES link (id) ON DELETE CASCADE,
    type            VARCHAR(16)   NOT NULL,             -- DEVICE | OS | GEO | AB
    match_value     VARCHAR(255),                       -- e.g. 'Mobile', 'iOS', 'IN,BD' (null for AB)
    destination_url VARCHAR(2048) NOT NULL,
    weight          INT           NOT NULL DEFAULT 1,   -- A-B split weighting
    priority        INT           NOT NULL DEFAULT 100, -- lower = evaluated first (non-AB)
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX ix_routing_rule_link ON routing_rule (link_id);
