package com.ritense.iko.zaken.searches

import com.ritense.iko.zaken.OpenZaakApi
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.RouteDefinition

abstract class OpenZaakSearch : RouteBuilder() {

    fun idAndSearchRoute(uri: String) {
        from(uri)
            .choice()
            .`when`(simple("\${header.id} != null"))
            .to("${uri}_id")
            .otherwise()
            .to("${uri}_search")
    }

    fun idRoute(
        uri: String,
        operation: String,
        id: String,
        func: (RouteDefinition) -> RouteDefinition = { i -> i }
    ) {
        from("${uri}_id")
            .removeHeaders("*", "id")
            .setHeader("openZaakApiOperation", constant(operation))
            .setHeader(id, header("id"))
            .let {
                func.invoke(it)
            }
            .to(OpenZaakApi.URI)
    }

    fun searchRoute(
        uri: String,
        operation: String,
        headers: List<String>,
        func: (RouteDefinition) -> RouteDefinition = { i -> i }
    ) {
        from("${uri}_search")
            .removeHeaders(
                "*", *headers.toTypedArray()
            )
            .setHeader("openZaakApiOperation", constant(operation))
            .let {
                func.invoke(it)
            }
            .to(OpenZaakApi.URI)
    }
}