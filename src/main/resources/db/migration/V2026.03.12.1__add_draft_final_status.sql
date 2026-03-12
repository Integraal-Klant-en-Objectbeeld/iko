-- Add status column to connector
ALTER TABLE connector ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'DRAFT';

-- Existing active versions are treated as finalized (already in production)
UPDATE connector SET status = 'FINAL' WHERE is_active = TRUE;

-- Add status column to aggregated_data_profile
ALTER TABLE aggregated_data_profile ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'DRAFT';

-- Existing active versions are treated as finalized (already in production)
UPDATE aggregated_data_profile SET status = 'FINAL' WHERE is_active = TRUE;
