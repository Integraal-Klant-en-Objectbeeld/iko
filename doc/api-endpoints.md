# API Endpoints

This document lists all HTTP endpoints exposed by IKO.

## Public API (JWT-secured)

These endpoints require a valid JWT bearer token with the appropriate roles.

### Aggregated Data Profiles

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/aggregated-data-profiles/{name}` | JWT (profile role) | Execute a profile and return aggregated data |

Query parameters are passed through to the primary endpoint via Camel headers.

### Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/endpoints/{name}` | JWT (`ROLE_ENDPOINT_*`) | Call a connector endpoint directly |

## Admin UI (OAuth2-secured)

All admin endpoints require OAuth2/OIDC login with at least one configured admin authority (default: `ROLE_ADMIN`, configurable via `iko.security.admin.authorities`). Roles are read from the OIDC **ID token** claim configured via `iko.security.admin.rolesClaim` (default: `roles`). Controllers are HTMX-aware: requests with `HX-Request: true` header receive Thymeleaf fragments; all other requests receive full pages with the master layout.

### Home

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin` | Admin dashboard with navigation menu |

### Aggregated Data Profiles Management

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/aggregated-data-profiles` | List profiles (paginated, default 10/page) |
| `GET` | `/admin/aggregated-data-profiles/{id}` | View profile details |
| `POST` | `/admin/aggregated-data-profiles` | Create new profile |
| `PUT` | `/admin/aggregated-data-profiles` | Update profile |
| `DELETE` | `/admin/aggregated-data-profiles/{id}` | Delete profile |
| `GET` | `/admin/aggregated-data-profiles/filter` | Search/filter profiles |
| `GET` | `/admin/aggregated-data-profiles/pagination` | Pagination navigation |

### Relation Management

| Method | Path | Description |
|---|---|---|
| `POST` | `/admin/relations` | Add relation to profile |
| `PUT` | `/admin/relations` | Update relation |
| `DELETE` | `/admin/relations` | Delete relation (cascades to children) |
| `GET` | `/admin/aggregated-data-profiles/{id}/relations/create` | Relation creation form |
| `GET` | `/admin/aggregated-data-profiles/{id}/relations/edit/{relationId}` | Relation edit form |
| `GET` | `/admin/aggregated-data-profiles/{id}/relations/edit/{relationId}/delete` | Relation delete confirmation |

### Cache Management

| Method | Path | Description |
|---|---|---|
| `DELETE` | `/admin/aggregated-data-profiles/{id}/cache` | Evict profile cache |
| `DELETE` | `/admin/aggregated-data-profiles/{id}/relation/{relationId}/cache` | Evict relation cache |

### Debug / Test

| Method | Path | Description |
|---|---|---|
| `POST` | `/admin/aggregated-data-profiles/debug` | Execute a profile with Camel tracing enabled. Returns JSON result and trace events. |

### Connector Management

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/connectors` | List all connectors |
| `GET` | `/admin/connectors/{id}` | View connector details |
| `POST` | `/admin/connectors` | Create new connector |
| `PUT` | `/admin/connectors/{id}` | Update connector YAML code |
| `DELETE` | `/admin/connectors/{id}` | Delete connector (cascades) |
| `GET` | `/admin/connectors/{id}/edit` | Connector code edit form |

### Connector Instance Management

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/connectors/{id}/instances/create` | Instance creation form |
| `POST` | `/admin/connectors/{id}/instances` | Create instance |
| `GET` | `/admin/connectors/{id}/instances/{instanceId}` | View instance details |
| `DELETE` | `/admin/connectors/{id}/instances/{instanceId}` | Delete instance |

### Connector Instance Configuration

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/connectors/{id}/instances/{instanceId}/config` | Configuration form |
| `POST` | `/admin/connectors/{id}/instances/{instanceId}/config` | Add config entry |
| `DELETE` | `/admin/connectors/{id}/instances/{instanceId}/config/{configKey}` | Delete config entry |
| `GET` | `/admin/connectors/{id}/instances/{instanceId}/config/{configKey}` | View config value |

### Connector Endpoint Management

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/connectors/{id}/endpoints/create` | Endpoint creation form |
| `POST` | `/admin/connectors/{id}/endpoints` | Create endpoint |
| `GET` | `/admin/connectors/{id}/endpoints/{endpointId}` | Edit endpoint |
| `DELETE` | `/admin/connectors/{id}/endpoints/{endpointId}` | Delete endpoint |

### Connector Endpoint Roles

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/connectors/{id}/instances/{instanceId}/roles` | Roles form |
| `POST` | `/admin/connectors/{id}/instances/{instanceId}/roles` | Add role mapping |
| `DELETE` | `/admin/connectors/{id}/instances/{instanceId}/roles/{roleId}` | Delete role mapping |

## Actuator Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/actuator/health` | Public | Application health check |
| `GET` | `/actuator/info` | Public | Application info |
| `GET` | `/actuator/prometheus` | `ROLE_ADMIN` | Prometheus metrics |

## Authentication Endpoints

These endpoints are publicly accessible:

| Method | Path | Description |
|---|---|---|
| `GET` | `/login` | OAuth2 login page |
| `GET` | `/oauth2/**` | OAuth2 callback handlers |
| `GET/POST` | `/logout` | Session logout |
