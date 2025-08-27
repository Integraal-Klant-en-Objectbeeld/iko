-- Create ConnectorEndpointRole table
CREATE TABLE connector_instance_config (
    connector_instance_id UUID         NOT NULL,
    key                   VARCHAR(255) NOT NULL,
    value                 VARCHAR(255) NOT NULL,
    FOREIGN KEY (connector_instance_id) REFERENCES connector_instance (id) ON DELETE CASCADE
);

