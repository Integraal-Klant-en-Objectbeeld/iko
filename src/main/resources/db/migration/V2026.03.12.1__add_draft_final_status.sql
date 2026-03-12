-- Add status column to connector
ALTER TABLE connector ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'DRAFT';

-- Add status column to aggregated_data_profile
ALTER TABLE aggregated_data_profile ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'DRAFT';
