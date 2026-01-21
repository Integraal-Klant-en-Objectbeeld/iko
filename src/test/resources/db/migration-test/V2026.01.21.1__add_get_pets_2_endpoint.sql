-- Add new 'Get Pets 2' endpoint
INSERT INTO connector_endpoint (id, name, connector_id, operation)
VALUES ('1862ffbb-5d5c-466e-807a-900ae57c9b4c', 'Get Pet 2', '9f3b9c7a-2d7c-4a1f-8f3d-1b9a2c7d4e5f', 'GetPet2');

-- Add ROLE_ADMIN access for the new endpoint
INSERT INTO connector_endpoint_role (id, connector_endpoint_id, connector_instance_id, role)
VALUES ('a3c7e9f1-2b4d-4f6a-8c0e-1d3f5a7b9c2e', '1862ffbb-5d5c-466e-807a-900ae57c9b4c', '7c4a2e5b-1f9d-4b3a-8c2e-6f1b3a7d9c0e', 'ROLE_ADMIN');