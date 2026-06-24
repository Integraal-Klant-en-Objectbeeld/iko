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

-- ============================================================================
-- Phase 4: baseline Aggregated Data Profiles
-- ============================================================================
-- Provide enough baseline ADP rows (>= one page of 10) so the ADP list spans
-- multiple pages regardless of which create/edit specs ran first, making the
-- pagination assertions order-independent. Mirrors the Connector baseline above.
--
-- Every profile references the single seeded connector instance + endpoint
-- (e2e00002-...01 / e2e00003-...01) so create/edit specs can reuse the same
-- known connectorInstanceId / connectorEndpointId the Add form pre-selects.
--
-- The ADP list defaults to "active only" and sorts by name ascending, so each
-- profile is inserted with is_active = TRUE and a zero-padded numeric name suffix
-- that sorts deterministically. They are status = FINAL so they are stable
-- (no Delete action, immutable) and never collide with the DRAFT rows the
-- create/edit specs add on top. (name, version) is unique and only one active
-- version per name is allowed; each baseline name is distinct so all stay active.
--
-- endpoint_transform / transform carry valid JQ (matching the Add form defaults:
-- '{}' for the endpoint transform, '.' for the result transform). roles is set
-- to ROLE_ADMIN, a realm role the e2e `admin` user holds.
INSERT INTO aggregated_data_profile (
    id, name, version, is_active, status,
    connector_instance_id, connector_endpoint_id,
    endpoint_transform, transform, roles, cache_enabled, cache_ttl
)
VALUES
    ('e2e00004-0000-0000-0000-000000000001', 'e2e-adp-01', '1.0.0', TRUE, 'FINAL',
     'e2e00002-0000-0000-0000-000000000001', 'e2e00003-0000-0000-0000-000000000001',
     '{}', '.', 'ROLE_ADMIN', FALSE, 0),
    ('e2e00004-0000-0000-0000-000000000002', 'e2e-adp-02', '1.0.0', TRUE, 'FINAL',
     'e2e00002-0000-0000-0000-000000000001', 'e2e00003-0000-0000-0000-000000000001',
     '{}', '.', 'ROLE_ADMIN', FALSE, 0),
    ('e2e00004-0000-0000-0000-000000000003', 'e2e-adp-03', '1.0.0', TRUE, 'FINAL',
     'e2e00002-0000-0000-0000-000000000001', 'e2e00003-0000-0000-0000-000000000001',
     '{}', '.', 'ROLE_ADMIN', FALSE, 0),
    ('e2e00004-0000-0000-0000-000000000004', 'e2e-adp-04', '1.0.0', TRUE, 'FINAL',
     'e2e00002-0000-0000-0000-000000000001', 'e2e00003-0000-0000-0000-000000000001',
     '{}', '.', 'ROLE_ADMIN', FALSE, 0),
    ('e2e00004-0000-0000-0000-000000000005', 'e2e-adp-05', '1.0.0', TRUE, 'FINAL',
     'e2e00002-0000-0000-0000-000000000001', 'e2e00003-0000-0000-0000-000000000001',
     '{}', '.', 'ROLE_ADMIN', FALSE, 0),
    ('e2e00004-0000-0000-0000-000000000006', 'e2e-adp-06', '1.0.0', TRUE, 'FINAL',
     'e2e00002-0000-0000-0000-000000000001', 'e2e00003-0000-0000-0000-000000000001',
     '{}', '.', 'ROLE_ADMIN', FALSE, 0),
    ('e2e00004-0000-0000-0000-000000000007', 'e2e-adp-07', '1.0.0', TRUE, 'FINAL',
     'e2e00002-0000-0000-0000-000000000001', 'e2e00003-0000-0000-0000-000000000001',
     '{}', '.', 'ROLE_ADMIN', FALSE, 0),
    ('e2e00004-0000-0000-0000-000000000008', 'e2e-adp-08', '1.0.0', TRUE, 'FINAL',
     'e2e00002-0000-0000-0000-000000000001', 'e2e00003-0000-0000-0000-000000000001',
     '{}', '.', 'ROLE_ADMIN', FALSE, 0),
    ('e2e00004-0000-0000-0000-000000000009', 'e2e-adp-09', '1.0.0', TRUE, 'FINAL',
     'e2e00002-0000-0000-0000-000000000001', 'e2e00003-0000-0000-0000-000000000001',
     '{}', '.', 'ROLE_ADMIN', FALSE, 0),
    ('e2e00004-0000-0000-0000-000000000010', 'e2e-adp-10', '1.0.0', TRUE, 'FINAL',
     'e2e00002-0000-0000-0000-000000000001', 'e2e00003-0000-0000-0000-000000000001',
     '{}', '.', 'ROLE_ADMIN', FALSE, 0),
    ('e2e00004-0000-0000-0000-000000000011', 'e2e-adp-11', '1.0.0', TRUE, 'FINAL',
     'e2e00002-0000-0000-0000-000000000001', 'e2e00003-0000-0000-0000-000000000001',
     '{}', '.', 'ROLE_ADMIN', FALSE, 0),
    ('e2e00004-0000-0000-0000-000000000012', 'e2e-adp-12', '1.0.0', TRUE, 'FINAL',
     'e2e00002-0000-0000-0000-000000000001', 'e2e00003-0000-0000-0000-000000000001',
     '{}', '.', 'ROLE_ADMIN', FALSE, 0);

-- ============================================================================
-- Phase 5: BRP personen connector + instance + endpoint + ADP (API flow)
-- ============================================================================
-- The public REST API specs (`/endpoints/...` and `/aggregated-data-profiles/...`)
-- need a connector whose route makes a real external call. This block seeds a
-- self-contained BRP "personen" connector that, when invoked, POSTs a
-- RaadpleegMetBurgerservicenummer search to the in-network personen mock
-- (`haalcentraal-personen:5010`) via Camel `rest-openapi`, using the bundled
-- OpenAPI document mounted at file:/openapi-specs/haalcentraal-brp-personen.yaml.
--
-- The connector route is deliberately simpler than the production
-- brp-wsgateway template (no Keycloak token exchange / mTLS): the e2e mock is
-- plain HTTP and unauthenticated. The route builds a minimal valid PersonenQuery
-- body so the GET `/endpoints/.../Personen` route resolves to a real 200 from the
-- mock. The mock answers any valid query with HTTP 200 and a `{ "type", "personen" }`
-- envelope (an unknown BSN simply yields an empty `personen` array), so the spec
-- can assert on the envelope without depending on a specific seeded BSN.
--
-- The connector tag (`e2e-brp`), instance tag (`e2e-brp-instance`) and operation
-- (`Personen`) are the path segments the `/endpoints/{connector}/{config}/{operation}`
-- route is invoked with.
INSERT INTO connector (id, name, tag, version, is_active, status, connector_code)
VALUES ('e2e00005-0000-0000-0000-000000000001', 'e2e-brp', 'e2e-brp', '1.0.0', TRUE, 'FINAL',
        '- route:
    id: "direct:iko:connector:e2e-brp"
    errorHandler:
        noErrorHandler: { }
    from:
        uri: "direct:iko:connector:e2e-brp"
        steps:
            - removeHeaders: "CamelHttp*"
            - setHeader:
                name: "CamelHttpMethod"
                constant: "POST"
            - setHeader:
                name: "Content-Type"
                constant: "application/json; charset=utf-8"
            - setHeader:
                name: "Accept"
                constant: "application/json; charset=utf-8"
            - setBody:
                constant: "{\"type\":\"RaadpleegMetBurgerservicenummer\",\"burgerservicenummer\":[\"999993653\"],\"fields\":[\"burgerservicenummer\",\"naam\"]}"
            - toD:
                uri: "language:groovy:\"rest-openapi:${variable.configProperties.apiSpecificationUrl}#${variable.operation}?host=${variable.configProperties.host}\""
            - unmarshal:
                json: { }');

-- Instance config (host + apiSpecificationUrl) stored as AES-GCM ciphertext,
-- encrypted with the fixed e2e IKO_CRYPTO_KEY that docker-compose-e2e.yaml sets.
-- Regenerate these values with e2e/seed/encrypt-config.mjs if the key changes.
INSERT INTO connector_instance (id, name, connector_id, tag, api_specification_url)
VALUES ('e2e00006-0000-0000-0000-000000000001', 'e2e-brp-instance',
        'e2e00005-0000-0000-0000-000000000001', 'e2e-brp-instance', NULL);

INSERT INTO connector_instance_config (connector_instance_id, key, value)
VALUES
    -- host: http://haalcentraal-personen:5010
    ('e2e00006-0000-0000-0000-000000000001', 'host',
     '4APAMKuUmMWgYj/gZSNb0k8DLyVwAyx2WY6j6QO2EDNBtKgyT2VuH2E8iZWu5yw5iifHbL8YvZY2lA97TA=='),
    -- apiSpecificationUrl: file:/openapi-specs/haalcentraal-brp-personen.yaml
    ('e2e00006-0000-0000-0000-000000000001', 'apiSpecificationUrl',
     'nWHGOMQbKmYpB2cyASAVUAD8G95PcwtaQUpEaBsgXAUlAIeTnDO4BiUq1Jblm4wzDVM/oH1Uwj1y/X1epjS+7gB6pOk+V2GK3l2uBKXr');

INSERT INTO connector_endpoint (id, name, connector_id, operation)
VALUES ('e2e00007-0000-0000-0000-000000000001', 'Personen',
        'e2e00005-0000-0000-0000-000000000001', 'Personen');

-- connector_endpoint_role grants ROLE_ADMIN on the BRP endpoint+instance pair so
-- the Camel per-endpoint role check (EndpointAuthRouteBuilder) passes for `admin`.
INSERT INTO connector_endpoint_role (id, connector_endpoint_id, connector_instance_id, role)
VALUES ('e2e00008-0000-0000-0000-000000000001',
        'e2e00007-0000-0000-0000-000000000001',
        'e2e00006-0000-0000-0000-000000000001',
        'ROLE_ADMIN');

-- An ADP exposing the same BRP endpoint via `/aggregated-data-profiles/{name}`.
-- roles=ROLE_ADMIN satisfies the per-profile Camel check (AuthRoute); the result
-- transform passes the mock envelope through unchanged ('.').
INSERT INTO aggregated_data_profile (
    id, name, version, is_active, status,
    connector_instance_id, connector_endpoint_id,
    endpoint_transform, transform, roles, cache_enabled, cache_ttl
)
VALUES
    ('e2e00009-0000-0000-0000-000000000001', 'e2e-brp-personen', '1.0.0', TRUE, 'FINAL',
     'e2e00006-0000-0000-0000-000000000001', 'e2e00007-0000-0000-0000-000000000001',
     '{}', '.', 'ROLE_ADMIN', FALSE, 0);
