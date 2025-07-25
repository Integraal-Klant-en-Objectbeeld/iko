package com.ritense.iko.connectors.bag.endpoints

import com.ritense.iko.connectors.bag.BagApi
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.RouteDefinition

abstract class BagEndpoint : RouteBuilder() {

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
            .setHeader("bagApiOperation", constant(operation))
            .setHeader(id, header("id"))
            .let {
                func.invoke(it)
            }
            .to(BagApi.URI)
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
            .setHeader("bagApiOperation", constant(operation))
            .let {
                func.invoke(it)
            }
            .to(BagApi.URI)
    }

}