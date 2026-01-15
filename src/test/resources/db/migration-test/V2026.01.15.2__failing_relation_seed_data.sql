-- Seed data for failing relation integration test
INSERT INTO connector_endpoint (id, name, connector_id, operation)
VALUES ('88888888-8888-8888-8888-888888888888', 'Get Pet Fail', '11111111-1111-1111-1111-111111111111', 'GetPetFail');

INSERT INTO aggregated_data_profile (id, name, connector_instance_id, connector_endpoint_id, transform, role, cache_enabled, cache_ttl)
VALUES ('99999999-9999-9999-9999-999999999999', 'test-failing-relation', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', '.', 'ROLE_ADMIN', false, 0);

INSERT INTO relation (id, aggregated_data_profile_id, property_name, source_id, source_to_endpoint_mapping, connector_instance_id, connector_endpoint_id, transform, cache_enabled, cache_ttl)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '99999999-9999-9999-9999-999999999999', 'pet1', '99999999-9999-9999-9999-999999999999', '{"petId": "$.id"}', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', '.', false, 0),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '99999999-9999-9999-9999-999999999999', 'pet2_failing', '99999999-9999-9999-9999-999999999999', '{"petId": "$.id"}', '22222222-2222-2222-2222-222222222222', '88888888-8888-8888-8888-888888888888', '.', false, 0);
