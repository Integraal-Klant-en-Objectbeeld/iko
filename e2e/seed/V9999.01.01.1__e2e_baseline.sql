-- ============================================================================
-- e2e baseline seed (mounted Flyway migration)
-- ============================================================================
-- This file is NOT part of the application's classpath migrations. It is mounted
-- into the e2e app container at /seed and applied via
--   SPRING_FLYWAY_LOCATIONS=classpath:db/migration,filesystem:/seed
-- (see docker-compose-e2e.yaml). Keeping it out of src/ guarantees it can never
-- run against a production database.
--
-- It mirrors the existing test-seed pattern
-- (src/test/resources/db/migration-test/V2026.01.08.1__adp_test_seed_data.sql):
-- plain INSERTs into connector / connector_instance / connector_endpoint.
--
-- Purpose for Phase 3: provide enough baseline Connector rows so the Connectors
-- list spans multiple pages regardless of which create/edit specs ran first, so
-- the pagination assertions are order-independent. The list defaults to
-- "active only" and sorts by name ascending, so every baseline connector is
-- inserted with is_active = TRUE, status = FINAL and a zero-padded numeric name
-- suffix that sorts deterministically.
--
-- All ids are fixed UUIDs (namespaced under the e2e0... prefix) so the data is
-- reproducible across runs. Names/tags are namespaced `e2e-conn-*` so they are
-- easy to filter on and never collide with UI-created rows.
--
-- Connector baseline rows carry valid connector code (a single direct route
-- whose URI matches the connector tag) so they pass any later validation, but
-- Phase 3 only asserts on list/search/paginate, not on route execution.

INSERT INTO connector (id, name, tag, version, is_active, status, connector_code)
VALUES
    ('e2e00001-0000-0000-0000-000000000001', 'e2e-conn-01', 'e2e-conn-01', '1.0.0', TRUE, 'FINAL',
     '- route:
    id: "direct:iko:connector:e2e-conn-01"
    errorHandler:
        noErrorHandler: { }
    from:
        uri: "direct:iko:connector:e2e-conn-01"
        steps:
            - log: "e2e baseline connector"'),
    ('e2e00001-0000-0000-0000-000000000002', 'e2e-conn-02', 'e2e-conn-02', '1.0.0', TRUE, 'FINAL',
     '- route:
    id: "direct:iko:connector:e2e-conn-02"
    errorHandler:
        noErrorHandler: { }
    from:
        uri: "direct:iko:connector:e2e-conn-02"
        steps:
            - log: "e2e baseline connector"'),
    ('e2e00001-0000-0000-0000-000000000003', 'e2e-conn-03', 'e2e-conn-03', '1.0.0', TRUE, 'FINAL',
     '- route:
    id: "direct:iko:connector:e2e-conn-03"
    errorHandler:
        noErrorHandler: { }
    from:
        uri: "direct:iko:connector:e2e-conn-03"
        steps:
            - log: "e2e baseline connector"'),
    ('e2e00001-0000-0000-0000-000000000004', 'e2e-conn-04', 'e2e-conn-04', '1.0.0', TRUE, 'FINAL',
     '- route:
    id: "direct:iko:connector:e2e-conn-04"
    errorHandler:
        noErrorHandler: { }
    from:
        uri: "direct:iko:connector:e2e-conn-04"
        steps:
            - log: "e2e baseline connector"'),
    ('e2e00001-0000-0000-0000-000000000005', 'e2e-conn-05', 'e2e-conn-05', '1.0.0', TRUE, 'FINAL',
     '- route:
    id: "direct:iko:connector:e2e-conn-05"
    errorHandler:
        noErrorHandler: { }
    from:
        uri: "direct:iko:connector:e2e-conn-05"
        steps:
            - log: "e2e baseline connector"'),
    ('e2e00001-0000-0000-0000-000000000006', 'e2e-conn-06', 'e2e-conn-06', '1.0.0', TRUE, 'FINAL',
     '- route:
    id: "direct:iko:connector:e2e-conn-06"
    errorHandler:
        noErrorHandler: { }
    from:
        uri: "direct:iko:connector:e2e-conn-06"
        steps:
            - log: "e2e baseline connector"'),
    ('e2e00001-0000-0000-0000-000000000007', 'e2e-conn-07', 'e2e-conn-07', '1.0.0', TRUE, 'FINAL',
     '- route:
    id: "direct:iko:connector:e2e-conn-07"
    errorHandler:
        noErrorHandler: { }
    from:
        uri: "direct:iko:connector:e2e-conn-07"
        steps:
            - log: "e2e baseline connector"'),
    ('e2e00001-0000-0000-0000-000000000008', 'e2e-conn-08', 'e2e-conn-08', '1.0.0', TRUE, 'FINAL',
     '- route:
    id: "direct:iko:connector:e2e-conn-08"
    errorHandler:
        noErrorHandler: { }
    from:
        uri: "direct:iko:connector:e2e-conn-08"
        steps:
            - log: "e2e baseline connector"'),
    ('e2e00001-0000-0000-0000-000000000009', 'e2e-conn-09', 'e2e-conn-09', '1.0.0', TRUE, 'FINAL',
     '- route:
    id: "direct:iko:connector:e2e-conn-09"
    errorHandler:
        noErrorHandler: { }
    from:
        uri: "direct:iko:connector:e2e-conn-09"
        steps:
            - log: "e2e baseline connector"'),
    ('e2e00001-0000-0000-0000-000000000010', 'e2e-conn-10', 'e2e-conn-10', '1.0.0', TRUE, 'FINAL',
     '- route:
    id: "direct:iko:connector:e2e-conn-10"
    errorHandler:
        noErrorHandler: { }
    from:
        uri: "direct:iko:connector:e2e-conn-10"
        steps:
            - log: "e2e baseline connector"'),
    ('e2e00001-0000-0000-0000-000000000011', 'e2e-conn-11', 'e2e-conn-11', '1.0.0', TRUE, 'FINAL',
     '- route:
    id: "direct:iko:connector:e2e-conn-11"
    errorHandler:
        noErrorHandler: { }
    from:
        uri: "direct:iko:connector:e2e-conn-11"
        steps:
            - log: "e2e baseline connector"'),
    ('e2e00001-0000-0000-0000-000000000012', 'e2e-conn-12', 'e2e-conn-12', '1.0.0', TRUE, 'FINAL',
     '- route:
    id: "direct:iko:connector:e2e-conn-12"
    errorHandler:
        noErrorHandler: { }
    from:
        uri: "direct:iko:connector:e2e-conn-12"
        steps:
            - log: "e2e baseline connector"');

-- One baseline instance + endpoint on the first connector. Not strictly required
-- for the Connectors list/search/paginate coverage, but gives the detail page a
-- populated instance/endpoint table and a stable instance for later phases to
-- reference.
INSERT INTO connector_instance (id, name, connector_id, tag, api_specification_url)
VALUES ('e2e00002-0000-0000-0000-000000000001', 'e2e-instance-01',
        'e2e00001-0000-0000-0000-000000000001', 'e2e-instance-01', NULL);

INSERT INTO connector_endpoint (id, name, connector_id, operation)
VALUES ('e2e00003-0000-0000-0000-000000000001', 'e2e-endpoint-01',
        'e2e00001-0000-0000-0000-000000000001', 'GetThings');
