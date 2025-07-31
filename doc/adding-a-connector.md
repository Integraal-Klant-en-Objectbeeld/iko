# Adding a New Connector to IKO

This guide provides step-by-step instructions for adding a new connector to the IKO (Integraal Klant & Objectbeeld) project.

## What is a Connector?

In the IKO project, a connector is a component that integrates with an external API to fetch and expose data. The project uses Apache Camel for routing and integration.

## Step 1: Create the Package Structure

Create a new package for your connector under `src/main/kotlin/com/ritense/iko/connectors/`. For example, if your connector is named "myapi", create:

```
src/main/kotlin/com/ritense/iko/connectors/myapi/
src/main/kotlin/com/ritense/iko/connectors/myapi/endpoints/
```

## Step 2: Create the Configuration Class

Create a configuration class that sets up the necessary beans for your connector. This class should be annotated with `@Configuration` and `@ConditionalOnProperty` to allow enabling/disabling the connector.

Example (`MyApiConfig.kt`):

```kotlin
@Configuration
@ConditionalOnProperty(
    value = ["iko.connectors.myapi.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class MyApiConfig {

    @Bean
    fun myapi(
        camelContext: CamelContext,
        @Value("\${iko.connectors.myapi.host}") host: String,
        @Value("\${iko.connectors.myapi.specificationUri}") specificationUri: URI
    ) =
        RestOpenApiComponent(camelContext).apply {
            this.specificationUri = specificationUri.toString()
            this.host = host
            this.produces = "application/json"
        }

    @Bean
    fun myApiApi() = MyApiApi()

    @Bean
    fun myApiPublicEndpoints() = MyApiPublicEndpoints()

    // Add beans for each endpoint
    @Bean
    fun myApiEndpointResource1() = MyApiEndpointResource1()

    @Bean
    fun myApiEndpointResource2() = MyApiEndpointResource2()
}
```

## Step 3: Create the API Interface

Create an API interface class that handles communication with the external API. This class should extend `RouteBuilder` and set up a route for API calls.

Example (`MyApiApi.kt`):

```kotlin
class MyApiApi : RouteBuilder() {

    companion object {
        val URI = "direct:myApiApi"
    }

    override fun configure() {
        from(URI)
            .errorHandler(noErrorHandler())
            .setHeader("Accept", constant("application/json"))
            .setHeader("Authorization", simple("Bearer {{iko.connectors.myapi.token}}"))
            .toD("myapi:\${header.myApiOperation}?throwExceptionOnFailure=false")
            .unmarshal().json()
    }
}
```

## Step 4: Create the Public Endpoints Class

Create a public endpoints class that exposes your connector's endpoints via REST. This class should extend `PublicEndpoints` and map public URL paths to internal endpoint URIs.

Example (`MyApiPublicEndpoints.kt`):

```kotlin
class MyApiPublicEndpoints : PublicEndpoints() {
    override fun configure() {
        handleAccessDeniedException()

        id("/myapi/resource1", MyApiEndpointResource1.URI)
        endpoint("/myapi/resource1", MyApiEndpointResource1.URI)

        id("/myapi/resource2", MyApiEndpointResource2.URI)
        endpoint("/myapi/resource2", MyApiEndpointResource2.URI)
    }
}
```

## Step 5: Create the Base Endpoint Class

Create a base endpoint class that provides common functionality for all your connector's endpoints. This class should extend `RouteBuilder` and provide methods for setting up routes.

Example (`MyApiEndpoint.kt`):

```kotlin
abstract class MyApiEndpoint : RouteBuilder() {

    fun idAndEndpointRoute(uri: String) {
        from(uri)
            .errorHandler(noErrorHandler())
            .choice()
            .`when`(simple("\${header.id} != null"))
            .to("${uri}_id")
            .otherwise()
            .to("${uri}_endpoint")
    }

    fun idRoute(
        uri: String,
        operation: String,
        id: String,
        func: (RouteDefinition) -> RouteDefinition = {i -> i}
    ) {
        from("${uri}_id")
            .errorHandler(noErrorHandler())
            .removeHeaders("*", "id")
            .setHeader("myApiOperation", constant(operation))
            .setHeader(id, header("id"))
            .let {
                func.invoke(it)
            }
            .to(MyApiApi.URI)
    }

    fun endpointRoute(
        uri: String,
        operation: String,
        headers: List<String>,
        func: (RouteDefinition) -> RouteDefinition = {i -> i}
    ) {
        from("${uri}_endpoint")
            .errorHandler(noErrorHandler())
            .removeHeaders(
                "*", *headers.toTypedArray()
            )
            .setHeader("myApiOperation", constant(operation))
            .let {
                func.invoke(it)
            }
            .to(MyApiApi.URI)
    }
}
```

## Step 6: Create Specific Endpoint Implementations

Create specific endpoint implementations for each API operation you want to expose. These classes should extend your base endpoint class and set up routes for specific operations.

Example (`MyApiEndpointResource1.kt`):

```kotlin
class MyApiEndpointResource1 : MyApiEndpoint() {
    companion object {
        val URI = "direct:myApiEndpointResource1"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "getResource1ById", "resource1Id") {
            it.setHeader("expand", constant("true"))
        }

        endpointRoute(
            URI, "searchResource1", listOf(
                "name",
                "type",
                "status",
                "page",
                "pageSize"
            )
        ) { routeDefinition ->
            routeDefinition.setHeader("Accept-Crs", constant("epsg:28992"))
        }
    }
}
```

Example (`MyApiEndpointResource2.kt`):

```kotlin
class MyApiEndpointResource2 : MyApiEndpoint() {
    companion object {
        val URI = "direct:myApiEndpointResource2"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "getResource2ById", "resource2Id")

        endpointRoute(
            URI, "searchResource2", listOf(
                "category",
                "status",
                "page",
                "pageSize"
            )
        )
    }
}
```

## Step 7: Add Configuration Properties

Add configuration properties for your connector to `application.yml`:

```yaml
iko:
  connectors:
    myapi:
      enabled: true
      host: # ENV
      specificationUri: "https://api.example.com/openapi.yaml"
      token: # ENV
```

## Step 8: Test Your Connector

Test your connector by:

1. Starting the application
2. Making requests to your exposed endpoints
3. Checking the logs for any errors

## Summary

Adding a new connector to IKO involves:

1. Creating a package structure
2. Creating a configuration class
3. Creating an API interface
4. Creating a public endpoints class
5. Creating a base endpoint class
6. Creating specific endpoint implementations
7. Adding configuration properties
8. Testing the connector

Each connector follows a similar pattern, making it easy to add new ones once you understand the structure.