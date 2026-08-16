-- Phase 4 (Day 13): link-in-bio — a hosted mini-page of links per workspace, served publicly at /bio/{slug}.
CREATE TABLE bio_page (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID         NOT NULL REFERENCES workspace (id),
    slug         VARCHAR(64)  NOT NULL UNIQUE,   -- global public namespace, like the default short host
    title        VARCHAR(255),
    avatar_url   VARCHAR(2048),
    bio          VARCHAR(500),
    theme        VARCHAR(32)  NOT NULL DEFAULT 'default',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE bio_block (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    bio_page_id UUID          NOT NULL REFERENCES bio_page (id) ON DELETE CASCADE,
    label       VARCHAR(255)  NOT NULL,
    url         VARCHAR(2048) NOT NULL,
    position    INT           NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX ix_bio_block_page ON bio_block (bio_page_id);
