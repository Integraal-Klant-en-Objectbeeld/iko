-- Add cache settings columns to relation
ALTER TABLE relation
    ADD COLUMN cache_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN cache_ttl INTEGER NOT NULL DEFAULT 0;
