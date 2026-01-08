-- Seed data for integration tests
INSERT INTO connector (id, name, tag, connector_code)
VALUES ('11111111-1111-1111-1111-111111111111', 'Test Connector', 'test-tag', 'test-code');

INSERT INTO connector_instance (id, name, connector_id, tag)
VALUES ('22222222-2222-2222-2222-222222222222', 'Test Instance', '11111111-1111-1111-1111-111111111111', 'test-instance-tag');

INSERT INTO connector_endpoint (id, name, connector_id, operation)
VALUES ('33333333-3333-3333-3333-333333333333', 'Test Endpoint', '11111111-1111-1111-1111-111111111111', 'test-operation');

INSERT INTO aggregated_data_profile (id, name, connector_instance_id, connector_endpoint_id, transform, role, cache_enabled, cache_ttl)
VALUES ('44444444-4444-4444-4444-444444444444', 'test', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', '.', 'ROLE_ADMIN', false, 0);
