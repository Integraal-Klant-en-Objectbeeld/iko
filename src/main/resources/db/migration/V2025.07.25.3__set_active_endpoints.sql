UPDATE endpoint
SET is_active = TRUE;

ALTER TABLE endpoint
    ALTER COLUMN is_active SET DEFAULT TRUE;