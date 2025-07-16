package com.ritense.iko.endpoints

import org.apache.camel.builder.RouteBuilder

abstract class PublicEndpoints: RouteBuilder() {

    fun id(uri: String, to: String) {
        rest("/endpoints$uri")
            .get("/{id}")
            .to("direct:${to}_id")

        from("direct:${to}_id")
            .errorHandler(noErrorHandler())
            .routeId("direct:${to}_api_id")
            .to(to)
            .marshal().json()
    }

    fun search(uri: String, to: String) {
        rest("/endpoints$uri")
            .get()
            .to("direct:${to}_search")

        from("direct:${to}_search")
            .errorHandler(noErrorHandler())
            .routeId("direct:${to}_api_search")
            .to(to)
            .marshal().json()
    }

}