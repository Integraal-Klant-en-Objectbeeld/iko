-- Additional seed data for integration tests
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
    '1a3c5e7f-9b2d-4c6e-8f1a-3c5e7f9b2d4f',
    'pet-household',
    '7c4a2e5b-1f9d-4b3a-8c2e-6f1b3a7d9c0e',
    '2b5f7c9d-4a1e-4d3b-9c2f-5e7a1b3d6f8c',
    '(if .filterParams?.pets?.id? then {"id": .filterParams.pets.id} else {} end)',
    '.right',
    'ROLE_ADMIN',
    false,
    0
);

INSERT INTO connector_endpoint (id, name, connector_id, operation)
VALUES ('3c6f8a1b-5d7e-4f2a-9b3c-6f8a1b5d7e9c', 'Get Owners', '9f3b9c7a-2d7c-4a1f-8f3d-1b9a2c7d4e5f', 'GetOwners');

INSERT INTO relation (id, aggregated_data_profile_id, property_name, source_id, source_to_endpoint_mapping, connector_instance_id, connector_endpoint_id, transform, cache_enabled, cache_ttl)
VALUES
    ('4d7e9f1a-3b5c-4d6e-8f7a-9b1c3d5e7f8a', '1a3c5e7f-9b2d-4c6e-8f1a-3c5e7f9b2d4f', 'owner', '1a3c5e7f-9b2d-4c6e-8f1a-3c5e7f9b2d4f', '{"id": .source.ownerId}', '7c4a2e5b-1f9d-4b3a-8c2e-6f1b3a7d9c0e', '3c6f8a1b-5d7e-4f2a-9b3c-6f8a1b5d7e9c', '.[0].name', false, 0),
    ('5e8f1a2b-6c7d-4e8f-9a1b-2c3d4e5f6a7b', '1a3c5e7f-9b2d-4c6e-8f1a-3c5e7f9b2d4f', 'pets', '1a3c5e7f-9b2d-4c6e-8f1a-3c5e7f9b2d4f', '{"ownerId": .source.ownerId, "ordering": .sortParams.pets.sort | if .direction == "DESC" then "-" + .property else .property end}', '7c4a2e5b-1f9d-4b3a-8c2e-6f1b3a7d9c0e', '2b5f7c9d-4a1e-4d3b-9c2f-5e7a1b3d6f8c', 'map(.name)', false, 0);
