-- Seed data for integration tests
INSERT INTO connector (id, name, tag, connector_code)
VALUES ('9f3b9c7a-2d7c-4a1f-8f3d-1b9a2c7d4e5f', 'Test Connector', 'pet', '' ||
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
VALUES ('7c4a2e5b-1f9d-4b3a-8c2e-6f1b3a7d9c0e', 'Test Instance', '9f3b9c7a-2d7c-4a1f-8f3d-1b9a2c7d4e5f', 'test-instance-tag');

INSERT INTO connector_endpoint (id, name, connector_id, operation)
VALUES ('2b5f7c9d-4a1e-4d3b-9c2f-5e7a1b3d6f8c', 'Get Pets', '9f3b9c7a-2d7c-4a1f-8f3d-1b9a2c7d4e5f', 'GetPets');

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
    '6d8f1b3c-5a7e-4c2d-9b1f-0a2c4e6f8d1b',
    'pets',
    '7c4a2e5b-1f9d-4b3a-8c2e-6f1b3a7d9c0e',
    '2b5f7c9d-4a1e-4d3b-9c2f-5e7a1b3d6f8c',
    '(if .filterParams?.pets?.ownerId? then {"ownerId": .filterParams.pets.ownerId} else {} end)
     + (if .sortParams?.pets?.pageNumber? then {"page": .sortParams.pets.pageNumber} else {} end)
     + (if .sortParams?.pets?.pageSize? then {"size": .sortParams.pets.pageSize} else {} end)
     + (if ((.sortParams?.pets?.sort? // []) | length) > 0
        then {"ordering": (.sortParams.pets.sort[0] | if .direction == "DESC" then "-" + .property else .property end)}
        else {} end)',
    'map(.name)',
    'ROLE_ADMIN',
    false,
    0
);

INSERT INTO connector_instance_config (connector_instance_id, key, value)
VALUES
    -- specificationUri: classpath:pet-api.yaml
    ('7c4a2e5b-1f9d-4b3a-8c2e-6f1b3a7d9c0e', 'specificationUri', 'OsOTfJr4Bv1dqOdaSJFRD1/wtHnDpEggmpmhq6cDggnxqJMZaCOzXBvvVgpka9clYhA='),
    -- host: http://localhost:10000
    ('7c4a2e5b-1f9d-4b3a-8c2e-6f1b3a7d9c0e', 'host', 'omGjhGPI9kmkD2wECM7rkQNm4Tk+Z5HDOXKG7K5DX2f8hTsDrDlVia/mTHrTg9cLChs=');
