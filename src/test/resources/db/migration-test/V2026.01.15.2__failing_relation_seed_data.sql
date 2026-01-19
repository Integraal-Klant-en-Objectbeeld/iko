-- Seed data for failing relation integration test
INSERT INTO connector_endpoint (id, name, connector_id, operation)
VALUES ('7f1a2b3c-4d5e-4f6a-8b7c-9d0e1f2a3b4c', 'Get Pet Fail', '9f3b9c7a-2d7c-4a1f-8f3d-1b9a2c7d4e5f', 'GetPetFail');

INSERT INTO aggregated_data_profile (
    id,
    name,
    connector_instance_id,
    connector_endpoint_id,
    endpoint_transform,
    transform,
    role,
    cache_enabled,
    cache_ttl
)
VALUES (
    '8a2b3c4d-5e6f-4a7b-9c8d-0e1f2a3b4c5d',
    'test-failing-relation',
    '7c4a2e5b-1f9d-4b3a-8c2e-6f1b3a7d9c0e',
    '2b5f7c9d-4a1e-4d3b-9c2f-5e7a1b3d6f8c',
    '(if .idParam? then {"id": .idParam} else {} end)',
    '.',
    'ROLE_ADMIN',
    false,
    0
);

INSERT INTO relation (id, aggregated_data_profile_id, property_name, source_id, source_to_endpoint_mapping, connector_instance_id, connector_endpoint_id, transform, cache_enabled, cache_ttl)
VALUES
    ('9b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e', '8a2b3c4d-5e6f-4a7b-9c8d-0e1f2a3b4c5d', 'pet1', '8a2b3c4d-5e6f-4a7b-9c8d-0e1f2a3b4c5d', '{"petId": "$.id"}', '7c4a2e5b-1f9d-4b3a-8c2e-6f1b3a7d9c0e', '2b5f7c9d-4a1e-4d3b-9c2f-5e7a1b3d6f8c', '.', false, 0),
    ('a1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e', '8a2b3c4d-5e6f-4a7b-9c8d-0e1f2a3b4c5d', 'pet2_failing', '8a2b3c4d-5e6f-4a7b-9c8d-0e1f2a3b4c5d', '{"petId": "$.id"}', '7c4a2e5b-1f9d-4b3a-8c2e-6f1b3a7d9c0e', '7f1a2b3c-4d5e-4f6a-8b7c-9d0e1f2a3b4c', '.', false, 0);
