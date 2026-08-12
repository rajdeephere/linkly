-- Phase 3 (Day 10): branded custom domains. Uniqueness becomes (domain, code) so two tenants can each
-- own the same code on different hostnames.

CREATE TABLE domain (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id       UUID         NOT NULL REFERENCES workspace (id),
    hostname           VARCHAR(255) NOT NULL UNIQUE,
    verified           BOOLEAN      NOT NULL DEFAULT false,
    verification_token VARCHAR(64),
    tls_status         VARCHAR(20)  NOT NULL DEFAULT 'pending',  -- pending | active | failed
    is_default         BOOLEAN      NOT NULL DEFAULT false,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- The default domain: the app's own short host. Existing + un-branded links live here.
INSERT INTO domain (id, workspace_id, hostname, verified, tls_status, is_default)
VALUES ('00000000-0000-0000-0000-0000000000d0',
        '00000000-0000-0000-0000-000000000001',
        'localhost:3000', true, 'active', true);

-- Attach every link to a domain (backfill existing rows to the default), then require it.
ALTER TABLE link ADD COLUMN domain_id UUID REFERENCES domain (id);
UPDATE link SET domain_id = '00000000-0000-0000-0000-0000000000d0';
ALTER TABLE link ALTER COLUMN domain_id SET NOT NULL;

-- Uniqueness moves from (code) to (domain_id, code).
DROP INDEX ux_link_code;
CREATE UNIQUE INDEX ux_link_domain_code ON link (domain_id, code);
