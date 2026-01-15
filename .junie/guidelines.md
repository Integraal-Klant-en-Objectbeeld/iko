# Spring Boot Guidelines (Kotlin)

## 1. Prefer Constructor Injection with Kotlin primary constructors
* Declare mandatory dependencies as constructor parameters and store them in `val` properties.
* Spring auto-detects the single primary constructor; no need for `@Autowired` on the constructor.
* Avoid field/setter injection in production code.

**Explanation:**

* With Kotlin, dependencies are typically `val` properties initialized via the primary constructor, ensuring objects are always in a valid state without framework-specific magic.
* You can unit test easily by passing test doubles into the constructor (no reflection required).
* The primary constructor clearly communicates a class’s dependencies.
* Spring Boot provides builder extension points such as `RestClient.Builder`, `ChatClient.Builder`, etc. With constructor injection you can configure and build these dependencies once.

```kotlin
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    builder: RestClient.Builder
) {
    private val restClient: RestClient = builder
        .baseUrl("http://catalog-service.com")
        .requestInterceptor(ClientCredentialTokenInterceptor())
        .build()

    // ... methods
}
```

Note: In Kotlin, classes and methods are `final` by default. Spring’s proxies require open classes/methods for certain features (e.g., transactions). Use the Kotlin Spring plugin (`org.jetbrains.kotlin.plugin.spring`) which automatically opens classes annotated with Spring stereotypes.

## 2. Prefer internal visibility for Spring components
* Kotlin has no package-private visibility. Use `internal` to restrict visibility to the module when possible (controllers, configuration, and `@Bean` methods don’t need to be `public` unless used externally).

**Explanation:**

* Restricting visibility (e.g., `internal`) reinforces encapsulation by hiding implementation details from other modules.
* Spring’s classpath scanning still detects and invokes components regardless of `internal` visibility within the same module.

## 3. Organize Configuration with Typed Properties
* Group application-specific configuration properties with a common prefix in `application.yml`/`properties`.
* Bind them to `@ConfigurationProperties` Kotlin classes with validation annotations so the app fails fast on invalid config.
* Prefer environment variables over profiles for environment-specific values.

**Explanation:**

* Centralizing configuration in a single `@ConfigurationProperties` bean consolidates names and validation rules.
* Scattering `@Value("${…}")` makes changes harder and error-prone.
* Overusing profiles creates ambiguity when multiple profiles are active.

Example:
```kotlin
@Validated
@ConfigurationProperties(prefix = "catalog")
data class CatalogProperties(
    @field:NotBlank val baseUrl: String,
)
```

In Spring Boot 3, constructor binding is the default for Kotlin data classes; no need for `@ConstructorBinding`.

## 4. Define Clear Transaction Boundaries
* Define each service-layer function as a transactional unit.
* Annotate query-only functions with `@Transactional(readOnly = true)`.
* Annotate data-modifying functions with `@Transactional`.
* Keep the work inside a transaction as small as possible.

**Explanation:**

* Single unit of work per use case ensures atomicity.
* A `@Transactional` function runs on a single DB connection for its scope.
* Read-only transactions avoid unnecessary dirty checking.
* Short transactions reduce lock contention.

## 5. Disable Open Session in View Pattern
* With Spring Data JPA, disable OSIV by setting `spring.jpa.open-in-view=false` in your properties/yml.

**Explanation:**

* OSIV can mask N+1 problems by loading lazy associations during view rendering/serialization.
* Disabling OSIV forces you to fetch only what you need via fetch joins, entity graphs, or tailored queries, reducing surprises and exceptions.

## 6. Separate Web Layer from Persistence Layer
* Do not expose entities directly from controllers.
* Define explicit request/response DTOs as Kotlin `data class` types.
* Apply Jakarta Validation annotations on request DTOs.

**Explanation:**

* Returning/binding entities couples your API to the database schema.
* DTOs specify exactly which fields clients can send/receive, improving clarity and security.
* Dedicated DTOs simplify validation.
* For mapping:
  - Kotlin makes simple mappings trivial via constructors and `copy()`.
  - If you prefer a mapper, MapStruct works with Kotlin using KAPT; alternatives include manual mapping or Kotlin DSLs.

## 7. Follow REST API Design Principles
* Versioned, resource-oriented URLs: `/api/v{version}/resources` (e.g., `/api/v1/orders`).
* Consistent patterns for collections and sub-resources (e.g., `/posts/{slug}/comments`).
* Return explicit HTTP status codes, typically via `ResponseEntity<T>`.
* Use pagination for unbounded collections.
* Use a JSON object as the top-level structure to allow future extension.
* Use either snake_case or camelCase consistently for JSON properties.

**Explanation:**

* Predictability and discoverability improve client experience.
* Standardized URLs, status codes, and headers enable reliable integrations.
* See also: Zalando RESTful API and Event Guidelines.

## 8. Use Command Objects for Business Operations
* Create purpose-built command DTOs (e.g., `CreateOrderCommand`) as Kotlin `data class` types.
* Accept these commands in service functions to drive creation/update workflows.

**Explanation:**

* Command/Query objects make expected input explicit.
* Avoids confusion about server-generated fields (IDs, timestamps).

## 9. Centralize Exception Handling
* Create a global handler annotated with `@RestControllerAdvice` (or `@ControllerAdvice`) and use `@ExceptionHandler` functions for specific exceptions.
* Return consistent error responses, consider ProblemDetails (RFC 9457).

**Explanation:**

* Handle all expected errors centrally and return standard responses.
* Avoid scattering try/catch blocks across controllers.

## 10. Actuator
* Expose only essential endpoints (e.g., `/health`, `/info`, `/metrics`) without auth; secure the rest.

**Explanation:**

* Health and metrics should be accessible to monitoring systems.
* In non-production (DEV/QA), you may temporarily expose more endpoints like `/actuator/beans` or `/actuator/loggers` for diagnostics.

## 11. Internationalization with ResourceBundles
* Externalize user-facing text (labels, prompts, messages) into ResourceBundles instead of hardcoding.

**Explanation:**

* Keeps translations manageable per locale.
* Spring resolves the appropriate bundle based on locale.

## 12. Use Testcontainers for integration tests
* Spin up real services (databases, brokers, etc.) during integration tests.

**Explanation:**

* Testcontainers reduces env inconsistencies by using the same type of dependencies as production.
* Pin to specific image versions, not `latest`.

## 13. Use random port for integration tests
* Start the application on a random available port to avoid conflicts:

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MyHttpTest {
    // tests
}
```

**Explanation:**

* CI servers may run tests in parallel on the same machine; random ports prevent conflicts.

## 14. Logging
* Use a logging framework through SLF4J; do not use `println`/`print` for application logs.
* Ensure sensitive data is never logged.
* Guard expensive log messages.

Kotlin example with SLF4J:
```kotlin
private val logger = org.slf4j.LoggerFactory.getLogger("app")

fun doWork() {
    if (logger.isDebugEnabled) {
        logger.debug("Detailed state: {}", computeExpensiveDetails())
    }
}
```

If you use Kotlin Logging (`io.github.oshai:kotlin-logging`), you can rely on lazy lambdas:
```kotlin
private val log = mu.KotlinLogging.logger {}

fun doWork() {
    log.debug { "Detailed state: ${'$'}{computeExpensiveDetails()}" }
}
```

**Explanation:**

* Logging frameworks let you tune log levels per environment.
* You can enrich logs with MDC and structured formats (e.g., JSON) for analysis.
* Guarding expensive messages avoids unnecessary overhead when the level is disabled.

## 15. File Formatting
* Do not add empty trailing lines to any file.

**Explanation:**

* Keeping files compact without unnecessary trailing whitespace helps in maintaining a consistent and dense code structure.