-- Add version and is_active columns to aggregated_data_profile
ALTER TABLE aggregated_data_profile ADD COLUMN version VARCHAR(50) NOT NULL DEFAULT '1.0.0';
ALTER TABLE aggregated_data_profile ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Add version and is_active columns to connector
ALTER TABLE connector ADD COLUMN version VARCHAR(50) NOT NULL DEFAULT '1.0.0';
ALTER TABLE connector ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Drop existing unique constraint on name (ADP)
ALTER TABLE aggregated_data_profile DROP CONSTRAINT IF EXISTS uq_profile_name;

-- Create new unique constraint on name + version combination
ALTER TABLE aggregated_data_profile ADD CONSTRAINT aggregated_data_profile_name_version_unique
    UNIQUE (name, version);

-- Drop existing unique constraint on tag (Connector) if it exists
ALTER TABLE connector DROP CONSTRAINT IF EXISTS connector_tag_key;

-- Create new unique constraint on tag + version combination
ALTER TABLE connector ADD CONSTRAINT connector_tag_version_unique
    UNIQUE (tag, version);

-- Only one version can be active per name (ADP)
-- Partial unique index: ensures only one is_active=true per name
CREATE UNIQUE INDEX idx_adp_name_active ON aggregated_data_profile (name)
    WHERE is_active = TRUE;

-- Only one version can be active per tag (Connector)
CREATE UNIQUE INDEX idx_connector_tag_active ON connector (tag)
    WHERE is_active = TRUE;
