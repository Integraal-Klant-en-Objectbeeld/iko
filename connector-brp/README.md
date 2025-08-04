# BRP Connector

This module provides integration with the BRP (Basisregistratie Personen) API.

## Features

- Person lookup by BSN
- Person search by various criteria (postcode, house number, name, etc.)
- Validation for BSN, postcode, and house number

## Configuration

The connector can be configured using the following properties in `application.yml`:

```yaml
iko:
  connectors:
    brp:
      enabled: true
      host: ${IKO_CONNECTORS_BRP_HOST:http://localhost:5010}
      specificationUri: ${IKO_CONNECTORS_BRP_SPECIFICATION_URI:https://developer.rvig.nl/brp-api/personen/_attachments/openapi.yaml}
```

Or using environment variables:

- `IKO_CONNECTORS_BRP_HOST`: The base URL of the BRP API
- `IKO_CONNECTORS_BRP_SPECIFICATION_URI`: The URL of the OpenAPI specification for the BRP API