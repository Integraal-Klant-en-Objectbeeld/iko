-- Add the column; it can be null, since the application sets a default when needed
ALTER TABLE aggregated_data_profile
    ADD COLUMN role VARCHAR(255);

-- Optional: set a default role for existing rows based on the profile name
UPDATE aggregated_data_profile
SET role = 'ROLE_AGGREGATED_DATA_PROFILE_' ||
           regexp_replace(UPPER(name), '[^0-9A-Z_-]', '', 'g')
WHERE role IS NULL;