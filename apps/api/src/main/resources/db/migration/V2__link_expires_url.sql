-- Day 4: optional fallback destination for an expired / click-capped link.
-- (expires_at, click_limit, click_count already exist from V1.)
ALTER TABLE link ADD COLUMN expires_url VARCHAR(2048);
