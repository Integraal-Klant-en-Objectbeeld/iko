# ObjectenAPI Connector

This module provides integration with the Objects API.

## Features

- Object retrieval by UUID
- Object listing with various filters

## Configuration

The connector can be configured using the following properties in `application.yml`:

```yaml
iko:
  connectors:
    objectenapi:
      enabled: true
      host: ${IKO_CONNECTORS_OBJECTENAPI_HOST:http://localhost:8000}
      specificationUri: ${IKO_CONNECTORS_OBJECTENAPI_SPECIFICATION_URI:https://raw.githubusercontent.com/maykinmedia/objects-api/3.1.2/src/objects/api/v2/openapi.yaml}
      token: ${IKO_CONNECTORS_OBJECTENAPI_TOKEN:token}
```

Or using environment variables:

- `IKO_CONNECTORS_OBJECTENAPI_HOST`: The base URL of the Objects API
- `IKO_CONNECTORS_OBJECTENAPI_SPECIFICATION_URI`: The URL of the OpenAPI specification for the Objects API
- `IKO_CONNECTORS_OBJECTENAPI_TOKEN`: The authentication token for the Objects API