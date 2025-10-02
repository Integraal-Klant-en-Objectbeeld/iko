-- Create Connector table
CREATE TABLE connector (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    tag VARCHAR(255)
);

-- Create ConnectorInstance table
CREATE TABLE connector_instance (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    connector_id UUID NOT NULL,
    tag VARCHAR(255),
    config TEXT,
    FOREIGN KEY (connector_id) REFERENCES connector(id) ON DELETE CASCADE
);

-- Create ConnectorEndpoint table
CREATE TABLE connector_endpoint (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    connector_id UUID NOT NULL,
    operation VARCHAR(255) NOT NULL,
    FOREIGN KEY (connector_id) REFERENCES connector(id) ON DELETE CASCADE
);