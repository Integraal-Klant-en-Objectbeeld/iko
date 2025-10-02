-- Create ConnectorEndpointRole table
CREATE TABLE connector_endpoint_role
(
    id                    UUID PRIMARY KEY,
    connector_endpoint_id UUID         NOT NULL,
    connector_instance_id UUID         NOT NULL,
    role                  VARCHAR(255) NOT NULL,
    FOREIGN KEY (connector_endpoint_id) REFERENCES connector_endpoint (id) ON DELETE CASCADE,
    FOREIGN KEY (connector_instance_id) REFERENCES connector_instance (id) ON DELETE CASCADE
);