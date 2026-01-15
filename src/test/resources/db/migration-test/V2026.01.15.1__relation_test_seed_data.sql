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
    '55555555-5555-5555-5555-555555555555',
    'pet-household',
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333',
    '(if .filterParams?.pets?.id? then {"id": .filterParams.pets.id} else {} end)',
    '.right',
    'ROLE_ADMIN',
    false,
    0
);

INSERT INTO connector_endpoint (id, name, connector_id, operation)
VALUES ('88888888-8888-8888-8888-888888888888', 'Get Owners', '11111111-1111-1111-1111-111111111111', 'GetOwners');

INSERT INTO relation (id, aggregated_data_profile_id, property_name, source_id, source_to_endpoint_mapping, connector_instance_id, connector_endpoint_id, transform, cache_enabled, cache_ttl)
VALUES
    ('66666666-6666-6666-6666-666666666666', '55555555-5555-5555-5555-555555555555', 'owner', '55555555-5555-5555-5555-555555555555', '{"id": .source.ownerId}', '22222222-2222-2222-2222-222222222222', '88888888-8888-8888-8888-888888888888', '.[0].name', false, 0),
    ('77777777-7777-7777-7777-777777777777', '55555555-5555-5555-5555-555555555555', 'pets', '55555555-5555-5555-5555-555555555555', '{"ownerId": .source.ownerId, "ordering": .sortParams.pets.sort | if .direction == "DESC" then "-" + .property else .property end}', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'map(.name)', false, 0);
