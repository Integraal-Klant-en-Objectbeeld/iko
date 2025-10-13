-- Widen value column to store AES-GCM Base64 payloads
ALTER TABLE relation
    add COLUMN property_name VARCHAR(255);

update relation set property_name = id;

alter table relation alter column property_name set not null;