-- ============================================================================
-- STEP 1: Handle duplicate connector tags BEFORE adding version columns
-- ============================================================================
-- The tag column was not unique, so duplicates may exist.
-- We rename duplicates by appending a suffix: tag_2, tag_3, etc.
-- The first occurrence (by id) keeps the original tag name.

-- Rename duplicate connector tags
WITH ranked_connectors AS (
    SELECT
        id,
        tag,
        ROW_NUMBER() OVER (PARTITION BY tag ORDER BY id) as rn
    FROM connector
),
duplicates_to_rename AS (
    SELECT
        id,
        tag,
        tag || '_' || rn as new_tag
    FROM ranked_connectors
    WHERE rn > 1
)
UPDATE connector c
SET tag = d.new_tag
FROM duplicates_to_rename d
WHERE c.id = d.id;

-- Also update the connector name to reflect the tag change (optional but recommended)
-- This keeps name and tag in sync for renamed connectors
WITH ranked_connectors AS (
    SELECT
        id,
        tag,
        name,
        ROW_NUMBER() OVER (PARTITION BY tag ORDER BY id) as rn
    FROM connector
),
duplicates_to_rename AS (
    SELECT
        id,
        name || ' (duplicate ' || rn || ')' as new_name
    FROM ranked_connectors
    WHERE rn > 1
)
UPDATE connector c
SET name = d.new_name
FROM duplicates_to_rename d
WHERE c.id = d.id;

-- ============================================================================
-- STEP 2: Add version and is_active columns
-- ============================================================================

-- Add version and is_active columns to aggregated_data_profile
ALTER TABLE aggregated_data_profile ADD COLUMN IF NOT EXISTS version VARCHAR(50) NOT NULL DEFAULT '1.0.0';
ALTER TABLE aggregated_data_profile ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Add version and is_active columns to connector
ALTER TABLE connector ADD COLUMN IF NOT EXISTS version VARCHAR(50) NOT NULL DEFAULT '1.0.0';
ALTER TABLE connector ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- ============================================================================
-- STEP 3: Create unique constraints
-- ============================================================================

-- Drop existing unique constraint on name (ADP) if it exists
ALTER TABLE aggregated_data_profile DROP CONSTRAINT IF EXISTS uq_profile_name;

-- Create new unique constraint on name + version combination
ALTER TABLE aggregated_data_profile ADD CONSTRAINT aggregated_data_profile_name_version_unique
    UNIQUE (name, version);

-- Drop existing unique constraint on tag (Connector) if it exists
ALTER TABLE connector DROP CONSTRAINT IF EXISTS connector_tag_key;

-- Create new unique constraint on tag + version combination
ALTER TABLE connector ADD CONSTRAINT connector_tag_version_unique
    UNIQUE (tag, version);

-- ============================================================================
-- STEP 4: Create partial unique indexes for active versions
-- ============================================================================

-- Only one version can be active per name (ADP)
-- Partial unique index: ensures only one is_active=true per name
CREATE UNIQUE INDEX idx_adp_name_active ON aggregated_data_profile (name)
    WHERE is_active = TRUE;

-- Only one version can be active per tag (Connector)
CREATE UNIQUE INDEX idx_connector_tag_active ON connector (tag)
    WHERE is_active = TRUE;
