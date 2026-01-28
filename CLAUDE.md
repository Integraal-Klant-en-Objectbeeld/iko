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

- **aggregateddataprofile/** — Core domain: data profiles that aggregate data from multiple external sources via connectors. Contains domain models, services, repositories, Camel processors, and serializers.
- **connectors/** — Connector management: defines connector instances, endpoints, and role-based endpoint access. Connector configurations are encrypted at rest (AES-GCM via `crypto/`).
- **crypto/** — AES-GCM encryption service and JPA `AttributeConverter` for transparent column encryption. Key provided via `IKO_CRYPTO_KEY` env var.
- **cache/** — Redis caching layer with Camel processor integration.
- **mvc/** — Spring MVC controllers (admin UI + REST API) and request/response DTOs.
- **security/** — OAuth2 client config (admin login) and JWT resource server config. Roles extracted from `resource_access.iko.roles` JWT claim.
- **camel/** — Camel framework configuration. YAML-based routes live in `src/main/resources/camel/*.yaml`.

### Key patterns

- **Dual auth:** OAuth2 login flow for admin panel (session-based), JWT tokens for API access (stateless).
- **Camel integration:** External systems are integrated via Apache Camel routes defined in YAML. Connectors are configured through the admin UI and their credentials are AES-GCM encrypted in PostgreSQL.
- **Server-rendered UI:** Thymeleaf templates with HTMX for partial updates. Full pages under `templates/pages/`, fragments for HTMX swaps under `templates/fragments/`. Uses IBM Carbon Design System web components and CSS.
- **Database migrations:** Flyway scripts in `src/main/resources/db/migration/`.

## Coding Conventions

- **Visibility:** Use `internal` instead of `public` for Spring components (controllers, services, configs). Spring classpath scanning still detects them.
- **Injection:** Constructor injection only (no `@Autowired`). Dependencies as `val` constructor parameters.
- **DTOs:** Separate request/response data classes; never expose JPA entities directly from controllers.
- **Transactions:** `@Transactional(readOnly = true)` for queries, `@Transactional` for mutations. OSIV is disabled.
- **Configuration:** Use `@ConfigurationProperties` with validation, not scattered `@Value` annotations.
- **Formatting:** Spotless enforces ktlint, prettier (HTML/CSS), and EUPL v1.2 license headers on all source files. Run `./gradlew spotlessApply` before committing.
- **No trailing blank lines** in any file.
- **Logging:** Use `kotlin-logging` (`io.github.oshai`) with lazy lambdas, never `println`.

## Testing

- Unit tests: standard JUnit 5, no special tag. Run with `./gradlew test`.
- Integration tests: tagged with `@Tag("integration")`. Require Docker Compose (`docker-compose-integration-test.yaml` is auto-started by Gradle). Run with `./gradlew integrationTest`.
- HTTP mocking: uses `MockWebServer` for simulating external HTTP endpoints.
- Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` for integration tests.

## Frontend

- **Templates:** Thymeleaf with layout dialect. Master layout: `templates/layout-internal.html`.
- **HTMX:** Progressive enhancement; pages must work without JS. Controllers return fragments when `HX-Request: true` header is present, full pages otherwise.
- **Carbon Design System:** Use `cds-*` web components and Carbon CSS grid/spacing tokens. Avoid hardcoded pixel values; use `var(--cds-spacing-*)`.
- **Static assets:** served from `src/main/resources/static/` at `/assets/**`.
