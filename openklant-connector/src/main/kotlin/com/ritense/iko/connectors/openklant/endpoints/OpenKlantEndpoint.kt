package com.ritense.iko.connectors.openklant.endpoints

import com.ritense.iko.connectors.openklant.OpenKlantApi
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.RouteDefinition

abstract class OpenKlantEndpoint : RouteBuilder() {

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
        id: String = "uuid",
        func: (RouteDefinition) -> RouteDefinition = { it }
    ) {
        from("${uri}_id")
            .errorHandler(noErrorHandler())
            .removeHeaders("*", "id")
            .setHeader("openKlantApiOperation", constant(operation))
            .setHeader(id, header("id"))
            .let(func)
            .to(OpenKlantApi.URI)
    }

    fun endpointRoute(
        uri: String,
        operation: String,
        headers: List<String> = listOf("page", "pageSize"),
        func: (RouteDefinition) -> RouteDefinition = { it }
    ) {
        from("${uri}_endpoint")
            .errorHandler(noErrorHandler())
            .removeHeaders("*", *headers.toTypedArray())
            .setHeader("openKlantApiOperation", constant(operation))
            .let(func)
            .to(OpenKlantApi.URI)
    }
}
