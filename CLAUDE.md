# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

IKO (Integraal Klant & Objectbeeld) is a Kotlin/Spring Boot application that aggregates data from external systems via Apache Camel connectors. It provides an admin UI (Thymeleaf + HTMX + IBM Carbon Design System) and REST APIs secured with OAuth2/JWT (Keycloak).

**Stack:** Kotlin 2.2, Spring Boot 3.5.x, JDK 21, Apache Camel, PostgreSQL, Redis, Flyway.

## Build & Test Commands

```bash
./gradlew build                # Build the application JAR
./gradlew test                 # Run unit tests (excludes @Tag("integration"))
./gradlew integrationTest      # Run integration tests (requires Docker Compose stack)
./gradlew check                # Run both unit + integration tests
./gradlew bootRun              # Run app locally (reads .env via Dotenv plugin)
./gradlew spotlessApply        # Auto-format code (ktlint + license headers + prettier)
./gradlew spotlessCheck        # Check formatting without modifying
```

**Local setup:** Copy `.env.template` to `.env`, generate `IKO_CRYPTO_KEY` (Base64 AES-256 key), add `127.0.0.1 keycloak` to `/etc/hosts`, then `docker compose up -d` and `./gradlew bootRun`.

**Running a single test:** `./gradlew test --tests "com.ritense.iko.SomeTestClass.someTestMethod"`

## Architecture

### Source layout (`src/main/kotlin/com/ritense/iko/`)

- **aggregateddataprofile/** — Core domain: data profiles that aggregate data from multiple external sources via connectors. Contains domain models (`AggregatedDataProfile`, `Relation`, `Roles`, `Transform`), services, repositories, Camel processors (`AggregatedDataProfileRoute`, `AggregatedDataProfileRouteBuilder`, `AuthRoute`), aggregation strategies (`MapAggregator`, `PairAggregator`), and serializers.
- **connectors/** — Connector management: `Connector` (route template), `ConnectorInstance` (deployed config with encrypted credentials), `ConnectorEndpoint` (operations), and `ConnectorEndpointRole` (RBAC). Configurations encrypted at rest (AES-GCM via `crypto/`).
- **crypto/** — AES-GCM encryption service and JPA `AttributeConverter` for transparent column encryption. Key provided via `IKO_CRYPTO_KEY` env var.
- **cache/** — Redis caching layer with `CacheService`, `CacheProcessor` (Camel integration), and domain types (`CacheEntry`, `CacheSettings`, `Cacheable`).
- **mvc/** — Spring MVC layer:
  - **controller/** — `AggregatedDataProfileController` (CRUD + relations + cache eviction), `ConnectorController` (connectors + instances + endpoints + roles + config), `HomeController`, `TestController` (debug/trace).
  - **model/** — Request/response DTOs and form objects. Never exposes JPA entities.
  - **model/validation/** — Custom validators: `UniqueAggregatedDataProfile`, `UniqueRelation`, `ValidTransform` (JQ expression validation).
- **security/** — `SecurityConfig` with three ordered filter chains: actuator (JWT), API (JWT), admin UI (OAuth2/OIDC). `SecurityContextHelper` for role utilities.
- **camel/** — Camel framework: `RestConfigurationRoute`, `GlobalErrorHandlerConfiguration`, `ErrorHelper`, `IkoConstants`, `IkoRouteHelper`.
- **json/** — Custom Jackson serializers.

### Key patterns

- **Dual auth:** OAuth2 login flow for admin panel (session-based), JWT tokens for API access (stateless). Three security filter chains ordered by specificity (actuator > API > admin).
- **Camel integration:** External systems are integrated via Apache Camel routes. Connector code is YAML-based Camel route definitions stored in the `Connector.connectorCode` column. Routes are built programmatically by `AggregatedDataProfileRouteBuilder`. Credentials are AES-GCM encrypted in PostgreSQL.
- **JQ transformations:** All data mapping between endpoints uses JQ 1.6 expressions (`net.thisptr.jackson.jq`). Endpoint transforms map parent data to endpoint parameters; result transforms shape the final output. Validated at entity construction time.
- **Hierarchical relations:** Relations form a tree structure (parent-child via `sourceId`). Cascade delete removes all descendants. The aggregation output uses `left`/`right` structure for combining parent and relation data.
- **Server-rendered UI:** Thymeleaf templates with HTMX for partial updates. Full pages under `templates/pages/`, fragments for HTMX swaps under `templates/fragments/`. Uses IBM Carbon Design System web components and CSS.
- **Database migrations:** Flyway scripts in `src/main/resources/db/migration/`, versioned as `Vyyyy.MM.DD.n__description.sql`.

### Domain model summary

- **AggregatedDataProfile** — Core entity: links a connector instance + endpoint, optional JQ transforms, roles for access control, cache settings, and a tree of relations.
- **Relation** — Child data source within a profile. Has its own connector instance + endpoint, JQ transforms, cache settings. Nested via `sourceId`.
- **Connector** — Template defining a Camel route (YAML). Has many endpoints and instances.
- **ConnectorInstance** — Deployed connector with encrypted `config` map (host, token, secrets).
- **ConnectorEndpoint** — Named operation within a connector (e.g., `zaak_list`, `personen`).
- **ConnectorEndpointRole** — Maps a role to an endpoint+instance pair for RBAC.

### Camel constants (`IkoConstants.kt`)

Key headers and variables used throughout Camel routes:
- `adp_profileName` — Profile name header for route lookup.
- `containerParam` — Container parameter header.
- `adp_endpointTransformContext` — Context for JQ endpoint transforms.
- `correlationId`, `iko_trace_id` — Tracing variables.

### Security filter chains

1. **Actuator** (`/actuator/**`): Health/info public, rest requires `ROLE_ADMIN`. JWT stateless.
2. **API** (`/endpoints/**`, `/aggregated-data-profiles/**`): JWT bearer tokens. Authorities from `resource_access.iko.roles` claim.
3. **Admin UI** (`/admin/**`): OAuth2/OIDC via Keycloak. Requires `ROLE_ADMIN`. CSRF disabled.

## Coding Conventions

- **Visibility:** Use `internal` instead of `public` for Spring components (controllers, services, configs). Spring classpath scanning still detects them.
- **Injection:** Constructor injection only (no `@Autowired`). Dependencies as `val` constructor parameters.
- **DTOs:** Separate request/response data classes; never expose JPA entities directly from controllers.
- **Transactions:** `@Transactional(readOnly = true)` for queries, `@Transactional` for mutations. OSIV is disabled.
- **Configuration:** Use `@ConfigurationProperties` with validation, not scattered `@Value` annotations.
- **Formatting:** Spotless enforces ktlint, prettier (HTML/CSS), and EUPL v1.2 license headers on all source files. Run `./gradlew spotlessApply` before committing.
- **No trailing blank lines** in any file.
- **Logging:** Use `kotlin-logging` (`io.github.oshai`) with lazy lambdas, never `println`.
- **JQ expressions:** Validated at entity construction. Use JQ 1.6 syntax. Single expressions return single results; array expressions cause batch execution of the relation.
- **Error handling:** Domain errors via sealed classes (`AggregatedDomainError`, `ConnectorDomainError`). Custom exceptions (`EndpointValidationFailed`, `AggregatedProfileNotFound`). Global Camel error handler for route exceptions.

## Testing

- Unit tests: standard JUnit 5, no special tag. Run with `./gradlew test`.
- Integration tests: tagged with `@Tag("integration")`. Require Docker Compose (`docker-compose-integration-test.yaml` is auto-started by Gradle). Run with `./gradlew integrationTest`.
- HTTP mocking: uses `MockWebServer` (`okhttp3`) for simulating external HTTP endpoints. Custom `MockWebServerExtension` JUnit 5 extension available.
- Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` for integration tests. Extend `BaseIntegrationTest` for common setup.
- Test config in `src/test/resources/application-test.yml` (separate DB/Redis ports, Camel tracing enabled, test crypto key).

## Frontend

- **Templates:** Thymeleaf with layout dialect. Master layout: `templates/layout-internal.html`.
- **HTMX:** Progressive enhancement; pages must work without JS. Controllers return fragments when `HX-Request: true` header is present, full pages otherwise.
- **Carbon Design System:** Use `cds-*` web components and Carbon CSS grid/spacing tokens. Avoid hardcoded pixel values; use `var(--cds-spacing-*)`.
- **Static assets:** served from `src/main/resources/static/` at `/assets/**`.
- **Monaco editor:** Integrated for JQ expression editing (`js/monaco-init.js`, `js/monaco-jq.js`).
- **JQ in browser:** Client-side JQ via WebAssembly (`js/jq.js`, `js/jq.wasm`).

## Key Dependencies

- **Apache Camel 4.17.x:** `camel-spring-boot`, `camel-yaml-dsl`, `camel-http`, `camel-rest`, `camel-jq`, `camel-jackson`, `camel-xml-jaxb`.
- **Security:** `spring-boot-starter-oauth2-client`, `spring-boot-starter-oauth2-resource-server`, `jjwt-api`/`jjwt-impl`/`jjwt-jackson`.
- **Database:** `postgresql`, `flyway-core`, `flyway-database-postgresql`, `spring-boot-starter-data-jpa`.
- **Cache:** `spring-boot-starter-data-redis`, `jedis`.
- **Frontend:** `spring-boot-starter-thymeleaf`, `thymeleaf-layout-dialect`.
- **Observability:** `spring-boot-starter-actuator`, `micrometer-registry-prometheus`.
- **Logging:** `kotlin-logging-jvm` (`io.github.oshai`).
- **Testing:** `spring-boot-starter-test`, `camel-test-spring-junit5`, `mockwebserver` (okhttp3), `spring-security-test`.
