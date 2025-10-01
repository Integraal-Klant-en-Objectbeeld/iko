-- Widen value column to store AES-GCM Base64 payloads
ALTER TABLE connector
    drop COLUMN description;

ALTER TABLE connector_instance
    drop column description;

ALTER TABLE connector_endpoint
    drop column description;