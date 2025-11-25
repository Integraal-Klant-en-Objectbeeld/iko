-- Add cache settings columns to aggregated_data_profile
ALTER TABLE aggregated_data_profile
    ADD COLUMN cache_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN cache_ttl INTEGER NOT NULL DEFAULT 0;
