-- Phase 4 (Day 12): scoped API keys for programmatic access. Only the SHA-256 hash is stored; the
-- plaintext key is shown once at creation. `prefix` is a non-secret display snippet.
CREATE TABLE api_key (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID        NOT NULL REFERENCES workspace (id),
    name         VARCHAR(255),
    prefix       VARCHAR(16) NOT NULL,
    hashed_key   VARCHAR(64) NOT NULL UNIQUE,
    role         VARCHAR(16) NOT NULL DEFAULT 'member',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at TIMESTAMPTZ
);
