UPDATE connector_instance
SET api_specification_url = 'classpath:pet-api.yaml'
WHERE id = '7c4a2e5b-1f9d-4b3a-8c2e-6f1b3a7d9c0e';

UPDATE connector
SET connector_code = REPLACE(connector_code, 'configProperties.specificationUri', 'configProperties.apiSpecificationUrl')
WHERE id = '9f3b9c7a-2d7c-4a1f-8f3d-1b9a2c7d4e5f';
