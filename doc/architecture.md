# Architecture

This document describes the high-level architecture of IKO (Integraal Klant & Objectbeeld).

## Overview

IKO is a data aggregation platform that fetches data from multiple external government/municipal systems and combines them into unified data profiles. It uses Apache Camel for integration routing, Spring Boot as the application framework, and provides both an admin UI and REST APIs.

```
                        +-------------------+
                        |   Admin UI        |
                        |   (Thymeleaf +    |
                        |    HTMX + Carbon) |
                        +--------+----------+
                                 |
                        OAuth2 (Keycloak)
                                 |
+----------------+      +--------v----------+      +------------------+
|  API Consumers | ---->|   IKO Application  |<---->|   PostgreSQL     |
|  (JWT tokens)  | JWT  |   (Spring Boot)   |      |   (Flyway)       |
+----------------+      +--------+----------+      +------------------+
                                 |
                        Apache Camel Routes
                                 |
              +------------------+------------------+
              |                  |                  |
     +--------v---+    +--------v---+    +---------v--+
     |  BRP       |    |  OpenZaak  |    |  Objects   |
     |  (Personen)|    |  (Zaken)   |    |  API       |
     +------------+    +------------+    +------------+
              |                  |
     +--------v---+    +--------v---+
     |  BAG       |    |  OpenKlant |
     |  (Adressen)|    |            |
     +------------+    +------------+
```

## Application Layers

### 1. Security Layer (`security/`)

Three ordered Spring Security filter chains handle authentication:

1. **Actuator chain** (highest precedence): Protects `/actuator/**`. Health and info endpoints are public; all others require `ROLE_ADMIN`. Uses JWT (stateless).
2. **API chain**: Protects `/endpoints/**` and `/aggregated-data-profiles/**`. Requires JWT bearer tokens. Authorities extracted from the `resource_access.iko.roles` JWT claim.
3. **Admin UI chain** (lowest precedence): Protects `/admin/**`. Uses OAuth2/OIDC login flow via Keycloak (session-based). Requires `ROLE_ADMIN`.

See [security.md](./security.md) for detailed configuration.

### 2. MVC Layer (`mvc/`)

Four controllers handle all HTTP traffic:

| Controller | Base Path | Purpose |
|---|---|---|
| `HomeController` | `/admin` | Admin dashboard and navigation |
| `AggregatedDataProfileController` | `/admin/aggregated-data-profiles` | Profile CRUD, relations, cache eviction |
| `ConnectorController` | `/admin/connectors` | Connector, instance, endpoint, and role management |
| `TestController` | `/admin/aggregated-data-profiles/debug` | Debug execution with Camel tracing |

Controllers are HTMX-aware: when the `HX-Request: true` header is present, they return Thymeleaf fragments for partial page updates. Otherwise, they return full pages with the master layout.

See [api-endpoints.md](./api-endpoints.md) for the complete endpoint reference.

### 3. Domain Layer

#### Aggregated Data Profiles (`aggregateddataprofile/`)

The core domain. An `AggregatedDataProfile` defines:
- A primary data source (connector instance + endpoint)
- Optional JQ transforms on the request context and final result
- Role-based access control
- A tree of `Relation` entities that fetch additional data from other endpoints
- Cache settings (TTL, enabled flag)

See [domain-model.md](./domain-model.md) for entity details.

#### Connectors (`connectors/`)

The connector subsystem manages external system integrations:
- `Connector` holds a YAML-based Camel route definition
- `ConnectorInstance` holds encrypted configuration (host, tokens, secrets)
- `ConnectorEndpoint` defines named operations within a connector
- `ConnectorEndpointRole` maps roles to endpoint+instance pairs

See [connectors/README.md](./connectors/README.md) for connector documentation.

### 4. Integration Layer (`camel/`, `aggregateddataprofile/camel/`)

Apache Camel handles all external system communication:

- **Route building**: `AggregatedDataProfileRouteBuilder` programmatically constructs Camel routes from connector YAML definitions and profile configuration.
- **Authentication**: `AuthRoute` handles token generation (e.g., JWT signing for OpenZaak/OpenDocumenten).
- **Aggregation**: `PairAggregator` combines parent and relation data into `left`/`right` structure. `MapAggregator` merges multiple relation results.
- **Caching**: `CacheProcessor` intercepts route execution to check/populate Redis cache.
- **Error handling**: `GlobalErrorHandlerConfiguration` provides centralized error handling for all routes.

### 5. Data Layer

#### PostgreSQL + Flyway

- JPA/Hibernate with PostgreSQL dialect
- OSIV disabled; explicit `@Transactional` management
- Flyway migrations in `src/main/resources/db/migration/` (versioned as `Vyyyy.MM.DD.n__description.sql`)
- Sensitive connector configuration encrypted at rest via `AesGcmStringAttributeConverter`

#### Redis Cache

- Spring Data Redis with Jedis client
- `CacheService` manages cache entries with configurable TTL
- Integrated into Camel routes via `CacheProcessor`
- Cache can be evicted per-profile or per-relation through the admin UI

See [caching.md](./caching.md) for details.

### 6. Encryption (`crypto/`)

- AES-GCM encryption via `AesGcmEncryptionService`
- JPA `AttributeConverter` (`AesGcmStringAttributeConverter`) provides transparent column-level encryption
- Key provided via `IKO_CRYPTO_KEY` environment variable (Base64-encoded AES-256 key)
- Used for connector instance configuration values (hosts, tokens, client secrets)

### 7. Frontend

- **Thymeleaf** with layout dialect; master layout at `templates/layout-internal.html`
- **HTMX** for progressive enhancement (partial page swaps without full reloads)
- **IBM Carbon Design System** web components and CSS grid/spacing tokens
- **Monaco editor** for JQ expression editing in the admin UI
- **JQ WebAssembly** for client-side JQ preview
- Static assets served from `src/main/resources/static/` at `/assets/**`

## Data Flow: Profile Execution

When a profile is requested via the API:

1. JWT token is validated; roles are checked against the profile's required roles.
2. The primary endpoint is called via its Camel route (connector instance config provides host/credentials).
3. The endpoint response is optionally transformed via the endpoint's JQ expression.
4. For each relation in the profile tree:
   a. The parent data is mapped to endpoint parameters via the relation's endpoint transform (JQ).
   b. The relation's endpoint is called via its Camel route.
   c. The relation response is optionally transformed via the relation's result transform.
   d. If the endpoint transform produces an array, the relation is executed once per element (batch).
5. Parent and relation data are aggregated into `left`/`right` structure.
6. The final result is optionally transformed via the profile's result transform (JQ).
7. Result is cached in Redis if caching is enabled for the profile.

## Configuration

- **Application config**: `src/main/resources/application.yml`
- **Environment variables**: `.env` file (loaded via Dotenv Gradle plugin for local dev)
- **Docker Compose**: `docker-compose.yaml` (full local stack), `docker-compose-integration-test.yaml` (test DB + Redis)

## CI/CD

- **PR validation**: `.github/workflows/validate-build.yml` — runs `./gradlew build` on pull requests
- **Snapshot releases**: `.github/workflows/snapshot-releases.yml` — builds and pushes Docker image to GHCR on merge to `main`
- **Manual releases**: `.github/workflows/start-manual-release.yml` — creates versioned Docker image and GitHub Release