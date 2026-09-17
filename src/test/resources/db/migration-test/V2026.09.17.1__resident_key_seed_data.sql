-- Seed data for the resident cache-key regression test.
--
-- The endpoint_transform deliberately omits .idParam: its JQ output is identical for
-- every resident. On the pre-fix code the cache key is built from that output only, so
-- two different residents (id=A and id=B) collide on a single Redis key. After the fix
-- the whole endpointTransformContext (which always carries idParam) feeds the key, so the
-- two residents produce two distinct keys.
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
    'b7f2c9d4-1e3a-4c5b-8d6e-2f4a6c8e0b1d',
    'resident-key-collision',
    '7c4a2e5b-1f9d-4b3a-8c2e-6f1b3a7d9c0e',
    '2b5f7c9d-4a1e-4d3b-9c2f-5e7a1b3d6f8c',
    '{"ordering": "name"}',
    'map(.name)',
    'ROLE_ADMIN',
    true,
    30000
);
