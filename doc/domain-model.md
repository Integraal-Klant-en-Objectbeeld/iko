# Domain Model

This document describes the core domain entities in IKO and their relationships.

## Entity Relationship Overview

```
+---------------------------+         +-------------------+
| AggregatedDataProfile     |         | Connector         |
|---------------------------|         |-------------------|
| id: UUID                  |    +--->| id: UUID          |
| name: String (unique)     |    |    | name: String      |
| connectorInstanceId: UUID-+----+    | tag: String       |
| connectorEndpointId: UUID-+----+    | connectorCode: T  |
| endpointTransform: JQ?    |    |    +--------+----------+
| resultTransform: JQ?      |    |             |
| roles: Roles              |    |    +--------v----------+    +---------------------+
| cacheSetting: CacheSetting|    |    | ConnectorEndpoint  |    | ConnectorInstance   |
+----------+----------------+    |    |--------------------|    |---------------------|
           |                     +--->| id: UUID           |    | id: UUID            |
           | 1:N                      | name: String       |    | name: String        |
           |                          | connector: FK      |    | connector: FK       |
+----------v----------------+         | operation: String  |    | tag: String         |
| Relation                  |         +--------------------+    | config: Map (encr.) |
|---------------------------|                  |                +----------+----------+
| id: UUID                  |                  |                           |
| propertyName: String      |         +--------v-----------+              |
| sourceId: UUID (parent)   |         | ConnectorEndpointRole|            |
| connectorInstanceId: UUID |         |---------------------|<------------+
| connectorEndpointId: UUID |         | id: UUID            |
| endpointTransform: JQ?    |         | connectorEndpoint:FK|
| resultTransform: JQ?      |         | connectorInstance:FK|
| cacheSetting: CacheSetting|         | role: String        |
+---------------------------+         +---------------------+
```

## Aggregated Data Profiles

### AggregatedDataProfile

The central entity. Represents a data profile that aggregates data from one or more external systems.

| Field | Type | Description |
|---|---|---|
| `id` | UUID | Primary key |
| `name` | String | Unique profile name, used in API paths |
| `connectorInstanceId` | UUID | Reference to the primary connector instance |
| `connectorEndpointId` | UUID | Reference to the primary endpoint |
| `endpointTransform` | EndpointTransform? | Optional JQ expression to transform the request context before calling the primary endpoint |
| `resultTransform` | Transform? | Optional JQ expression to transform the final aggregated result |
| `roles` | Roles | Comma-separated list of roles required to access this profile |
| `relations` | Set\<Relation\> | Child relations (hierarchical tree) |
| `aggregatedDataProfileCacheSetting` | CacheSetting | Cache TTL and enabled flag |

Key behaviors:
- `handle(request)` updates the profile from a form submission.
- `addRelation()` adds a new relation to the profile tree.
- `changeRelation()` updates an existing relation.
- `removeRelation()` removes a relation and all its descendants.
- `level1Relations()` returns top-level relations (where `sourceId` matches the profile `id`).
- `relationsOf(id)` returns child relations for a given parent.

### Relation

A child data source that fetches additional data to enrich the parent profile or relation.

| Field | Type | Description |
|---|---|---|
| `id` | UUID | Primary key |
| `propertyName` | String | Property name in the output JSON (e.g., `"address"`, `"cases"`) |
| `sourceId` | UUID | Parent ID (profile ID for top-level, relation ID for nested) |
| `connectorInstanceId` | UUID | Connector instance to use |
| `connectorEndpointId` | UUID | Endpoint to call |
| `endpointTransform` | RelationEndpointTransform? | JQ expression mapping parent data to endpoint parameters |
| `resultTransform` | Transform? | JQ expression to transform the relation's result |
| `relationCacheSettings` | RelationCacheSettings | Cache TTL and enabled flag |

Relations form a tree via `sourceId`:
- Top-level relations have `sourceId` = profile ID.
- Nested relations have `sourceId` = parent relation ID.
- Deleting a relation cascades to all descendants.

### Transform / EndpointTransform / RelationEndpointTransform

Embeddable value objects that hold JQ expressions. Validated at construction time using `net.thisptr.jackson.jq.JsonQuery` (JQ 1.6).

- **EndpointTransform**: Maps request context to primary endpoint parameters.
- **RelationEndpointTransform**: Maps parent response data to relation endpoint parameters. If the expression produces an array, the relation is executed once per element (batch mode).
- **Transform**: General-purpose result transformation.

### Roles

Embeddable value object storing a comma-separated list of role names.

- Pattern: `^[A-Za-z0-9_-]+(,[A-Za-z0-9_-]+)*$`
- Example: `"ROLE_USER,ROLE_ADMIN"`
- `asList()` splits into a list for programmatic access.

### CacheSetting / RelationCacheSettings

Embeddable value objects for cache configuration.

| Field | Type | Description |
|---|---|---|
| `enabled` | Boolean | Whether caching is active |
| `timeToLive` | Int | Cache TTL in milliseconds |

## Connectors

### Connector

A template defining an external system integration.

| Field | Type | Description |
|---|---|---|
| `id` | UUID | Primary key |
| `name` | String | Display name (e.g., "OpenZaak", "BRP") |
| `tag` | String | Reference tag used in Camel route resolution |
| `connectorCode` | Text | YAML-based Camel route definition |

The `connectorCode` field contains a complete Camel YAML route definition that describes how to communicate with the external system (HTTP configuration, headers, authentication, parameter mapping).

### ConnectorInstance

A deployed instance of a connector with specific configuration (credentials, host, etc.).

| Field | Type | Description |
|---|---|---|
| `id` | UUID | Primary key |
| `name` | String | Instance name |
| `connector` | Connector (FK) | Parent connector template |
| `tag` | String | Instance alias/tag |
| `config` | Map\<String, String\> | Configuration key-value pairs, encrypted at rest (AES-GCM) |

Configuration values typically include:
- `host` — Base URL of the external system
- `specificationUri` — OpenAPI specification path
- `token` / `secret` — API authentication credentials
- `clientId` / `clientSecret` — OAuth2 client credentials

All values in the `config` map are transparently encrypted/decrypted by `AesGcmStringAttributeConverter`.

### ConnectorEndpoint

A named operation within a connector.

| Field | Type | Description |
|---|---|---|
| `id` | UUID | Primary key |
| `name` | String | Display name |
| `connector` | Connector (FK) | Parent connector |
| `operation` | String | Operation identifier used in Camel routes |

### ConnectorEndpointRole

Maps a required role to a specific endpoint + instance combination for access control.

| Field | Type | Description |
|---|---|---|
| `id` | UUID | Primary key |
| `connectorEndpoint` | ConnectorEndpoint (FK) | The endpoint |
| `connectorInstance` | ConnectorInstance (FK) | The instance |
| `role` | String | Required role name |

## Cache Domain

### CacheEntry

An event representing a cache operation.

| Field | Type | Description |
|---|---|---|
| `type` | Enum | `PUT`, `HIT`, or `MISS` |
| `key` | String | Cache key |
| `value` | String? | Cached value (present for PUT and HIT) |
| `timeToLive` | Duration | TTL for the entry |

### Cacheable

Interface with extension functions that convert `AggregatedDataProfile` and `Relation` entities into cacheable objects. The cache key is computed from profile/relation ID, JQ expressions, and endpoint result data.

## Database Tables

Key tables (managed by Flyway migrations):

| Table | Entity |
|---|---|
| `aggregated_data_profile` | AggregatedDataProfile |
| `relation` | Relation |
| `connector` | Connector |
| `connector_instance` | ConnectorInstance |
| `connector_instance_config` | ConnectorInstance config entries |
| `connector_endpoint` | ConnectorEndpoint |
| `connector_endpoint_role` | ConnectorEndpointRole |