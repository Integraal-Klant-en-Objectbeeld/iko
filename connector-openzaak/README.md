# OpenZaak Connector

This module provides integration with the OpenZaak API.

## Features

- Zaak (case) management
- Role management
- Status management
- Result management
- Object management
- Document management
- Contact moment management
- Request management

## Configuration

The connector can be configured using the following properties in `application.yml`:

```yaml
iko:
  connectors:
    openzaak:
      enabled: true
      host: ${IKO_CONNECTORS_OPENZAAK_HOST:http://localhost:8001}
      specificationUri: ${IKO_CONNECTORS_OPENZAAK_SPECIFICATION_URI:https://raw.githubusercontent.com/vng-Realisatie/zaken-api/1.5.1/src/openapi.yaml}
      clientId: ${IKO_CONNECTORS_OPENZAAK_CLIENT_ID:iko}
      secret: ${IKO_CONNECTORS_OPENZAAK_SECRET:secret}
```

Or using environment variables:

- `IKO_CONNECTORS_OPENZAAK_HOST`: The base URL of the OpenZaak API
- `IKO_CONNECTORS_OPENZAAK_SPECIFICATION_URI`: The URL of the OpenAPI specification for the OpenZaak API
- `IKO_CONNECTORS_OPENZAAK_CLIENT_ID`: The client ID for authentication
- `IKO_CONNECTORS_OPENZAAK_SECRET`: The secret for authentication