# Connectors

Connectors are Apache Camel route definitions that allow IKO to connect to external systems. Each connector is a reusable template that can be deployed as one or more instances with different configurations.

## Concepts

### Connector

A connector defines *how* to communicate with an external system. It contains a YAML-based Camel route definition (`connectorCode`) that describes HTTP configuration, header mapping, authentication, and parameter handling.

This list contains the configuration examples:

- [BAG](./bag.md) -- Basisregistratie Adressen en Gebouwen (addresses and buildings)
- [BRP](./haalcentraal-brp.md) -- Haal Centraal BRP (persons registry)
- [ObjectenAPI](objectenapi.md) -- Objects API
- [OpenDocumenten](opendocumenten.md) -- Open Documenten (document management)
- [OpenKlant](openklant.md) -- Open Klant (customer contacts)
- [OpenZaak](openzaak.md) -- Open Zaak (case management)
- [Demo](demo.md) -- Demo connector with mock data

### Connector Instance

A connector instance is a deployment of a connector with specific configuration. Each instance has its own set of encrypted key-value configuration entries (host, credentials, tokens, etc.).

For example, you might have one "OpenZaak" connector but two instances pointing to different environments (test and production).

Configuration values are stored encrypted in the database using AES-GCM. See [security.md](../security.md) for details.

Typical configuration keys:

| Key | Description |
|---|---|
| `host` | Base URL of the external system |
| `specificationUri` | OpenAPI specification path |
| `token` / `secret` | API authentication token |
| `clientId` | OAuth2 client ID |
| `clientSecret` | OAuth2 client secret |

### Connector Endpoint

A connector endpoint is a named operation within a connector. It maps to a specific API operation or route. For example, the OpenZaak connector has endpoints like `zaak_list`, `zaak_read`, `zaakinformatieobject_list`.

Endpoints are referenced by `AggregatedDataProfile` and `Relation` entities to define which external operation to call.

### Connector Endpoint Roles

Role-based access control for endpoints. Each role mapping associates a required role with a specific endpoint + instance combination. Users must have the corresponding role in their JWT token to access the endpoint.

## Connector Code (YAML Routes)

Connector code is written in Apache Camel YAML DSL. The route definition typically includes:

- **HTTP endpoint configuration**: Base URL, path, method
- **Header mapping**: Maps Camel exchange headers to HTTP request headers/parameters
- **Authentication**: Token injection or JWT generation (e.g., HS256 for OpenZaak)
- **Parameter filtering**: Whitelisting which headers are forwarded as query parameters

Configuration values from the connector instance are injected using `REFERENCE` placeholders in the YAML route, which are resolved at runtime from the encrypted config store.

## Managing Connectors

Connectors, instances, endpoints, and roles are managed through the admin UI at `/admin/connectors`. The connector code editor uses Monaco for YAML editing with syntax highlighting.

See [api-endpoints.md](../api-endpoints.md) for the full list of connector management HTTP endpoints.