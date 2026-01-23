-- Seed data for endpoint transform result type validation integration test
INSERT INTO aggregated_data_profile (
    id,
    name,
    connector_instance_id,
    connector_endpoint_id,
    endpoint_transform,
    transform,
    roles,
    cache_enabled,
    cache_ttl
)
VALUES (
    'c4c3b2a1-0d9e-8f7a-6b5c-4d3e2f1a0b9c',
    'endpoint-transform-result-array',
    '7c4a2e5b-1f9d-4b3a-8c2e-6f1b3a7d9c0e',
    '2b5f7c9d-4a1e-4d3b-9c2f-5e7a1b3d6f8c',
    '[]',
    '.',
    'ROLE_ADMIN',
    false,
    0
);
