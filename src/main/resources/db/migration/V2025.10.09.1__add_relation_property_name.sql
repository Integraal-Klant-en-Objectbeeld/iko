-- Widen value column to store AES-GCM Base64 payloads
ALTER TABLE relation
    ADD COLUMN property_name VARCHAR(255);

UPDATE relation SET property_name = id;

ALTER TABLE relation ALTER COLUMN property_name SET NOT NULL;