-- Additional seed data for integration tests
INSERT INTO aggregated_data_profile (id, name, connector_instance_id, connector_endpoint_id, transform, role, cache_enabled, cache_ttl)
VALUES ('55555555-5555-5555-5555-555555555555', 'test-with-relations', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', '.', 'ROLE_ADMIN', false, 0);

INSERT INTO relation (id, aggregated_data_profile_id, property_name, source_id, source_to_endpoint_mapping, connector_instance_id, connector_endpoint_id, transform, cache_enabled, cache_ttl)
VALUES
    ('66666666-6666-6666-6666-666666666666', '55555555-5555-5555-5555-555555555555', 'pet1', '55555555-5555-5555-5555-555555555555', '{"petId": "$.id"}', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', '.', false, 0),
    ('77777777-7777-7777-7777-777777777777', '55555555-5555-5555-5555-555555555555', 'pet2', '55555555-5555-5555-5555-555555555555', '{"petId": "$.id"}', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', '.', false, 0);

INSERT INTO aggregated_data_profile (id, name, connector_instance_id, connector_endpoint_id, transform, role, cache_enabled, cache_ttl)
VALUES ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'test-cached', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', '.', 'ROLE_ADMIN', true, 30000);
