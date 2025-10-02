-- Widen value column to store AES-GCM Base64 payloads
ALTER TABLE connector_instance_config
    ALTER COLUMN value TYPE TEXT;