package com.ritense.iko.search

import org.apache.camel.builder.RouteBuilder

abstract class PublicSearchEndpoints: RouteBuilder() {

    fun id(uri: String, to: String) {
        rest("/searches$uri")
            .get("/{id}")
            .to("direct:${to}_rest_id")

        from("direct:${to}_rest_id")
            .to(to)
            .marshal().json()
    }

    fun search(uri: String, to: String) {
        rest("/searches$uri")
            .get()
            .to("direct:${to}_rest_search")

        from("direct:${to}_rest_search")
            .routeId("direct:${to}_rest_search")
            .to(to)
            .marshal().json()
    }

}