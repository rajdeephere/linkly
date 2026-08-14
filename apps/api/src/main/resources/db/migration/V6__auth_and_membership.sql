-- Phase 4 (Day 12): authentication + team membership.
ALTER TABLE app_user ADD COLUMN password_hash VARCHAR(100);

CREATE TABLE membership (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID        NOT NULL REFERENCES workspace (id),
    user_id      UUID        NOT NULL REFERENCES app_user (id),
    role         VARCHAR(16) NOT NULL DEFAULT 'member',  -- owner | admin | member
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (workspace_id, user_id)
);

CREATE INDEX ix_membership_user ON membership (user_id);
