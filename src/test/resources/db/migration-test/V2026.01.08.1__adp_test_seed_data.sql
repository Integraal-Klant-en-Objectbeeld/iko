-- Seed data for integration tests
INSERT INTO connector (id, name, tag, connector_code)
VALUES ('11111111-1111-1111-1111-111111111111', 'Test Connector', 'pet', '' ||
'-   route:
        id: "direct:iko:connector:pet"
        errorHandler:
            noErrorHandler: { }
        from:
            uri: "direct:iko:connector:pet"
            steps:
                - removeHeaders: "CamelHttp*"
                - log: "header: ${header.Accept}"
                - toD:
                    uri: "language:groovy:\"rest-openapi:${variable.configProperties.specificationUri}#${variable.operation}?host=${variable.configProperties.host}\""
                - log: "body: ${body}"
                - unmarshal:
                    json: { }' ||
'');

INSERT INTO connector_instance (id, name, connector_id, tag)
VALUES ('22222222-2222-2222-2222-222222222222', 'Test Instance', '11111111-1111-1111-1111-111111111111', 'test-instance-tag');

INSERT INTO connector_endpoint (id, name, connector_id, operation)
VALUES ('33333333-3333-3333-3333-333333333333', 'Get Pets', '11111111-1111-1111-1111-111111111111', 'GetPets');

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
    '44444444-4444-4444-4444-444444444444',
    'pets',
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333',
    '(if .filterParams?.pets?.ownerId? then {"ownerId": .filterParams.pets.ownerId} else {} end)
     + (if .sortParams?.pets?.pageNumber? then {"page": .sortParams.pets.pageNumber} else {} end)
     + (if .sortParams?.pets?.pageSize? then {"size": .sortParams.pets.pageSize} else {} end)',
    'map(.name)',
    'ROLE_ADMIN',
    false,
    0
);

INSERT INTO connector_instance_config (connector_instance_id, key, value)
VALUES
    -- specificationUri: classpath:pet-api.yaml
    ('22222222-2222-2222-2222-222222222222', 'specificationUri', 'OsOTfJr4Bv1dqOdaSJFRD1/wtHnDpEggmpmhq6cDggnxqJMZaCOzXBvvVgpka9clYhA='),
    -- host: http://localhost:10000
    ('22222222-2222-2222-2222-222222222222', 'host', 'omGjhGPI9kmkD2wECM7rkQNm4Tk+Z5HDOXKG7K5DX2f8hTsDrDlVia/mTHrTg9cLChs=');
