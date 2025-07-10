package com.ritense.iko.search

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException

abstract class PublicSearchEndpoints : RouteBuilder() {

    fun id(uri: String, to: String) {
        rest("/searches$uri")
            .get("/{id}")
            .to("direct:${to}_rest_id")

        from("direct:${to}_rest_id")
            .errorHandler(noErrorHandler())
            .setVariable("authorities", constant("ROLE_SEARCH_${to.replace("direct:", "").uppercase()}"))
            .to("direct:auth")
            .to(to)
            .marshal().json()
    }

    fun search(uri: String, to: String) {
        rest("/searches$uri")
            .get()
            .to("direct:${to}_rest_search")

        from("direct:${to}_rest_search")
            .errorHandler(noErrorHandler())
            .setVariable("authorities", constant("ROLE_SEARCH_${to.replace("direct:", "").uppercase()}"))
            .to("direct:auth")
            .routeId("direct:${to}_rest_search")
            .to(to)
            .marshal().json()
    }

    fun handleAccessDeniedException() {
        onException(AccessDeniedException::class.java)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpStatus.UNAUTHORIZED.value()))
    }
}