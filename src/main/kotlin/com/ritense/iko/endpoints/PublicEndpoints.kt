package com.ritense.iko.endpoints

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.springframework.http.HttpStatus

abstract class PublicEndpoints : RouteBuilder() {

    fun id(uri: String, to: String) {
        rest("/endpoints$uri")
            .get("/{id}")
            .to("direct:${to}_id")

        from("direct:${to}_id")
            .errorHandler(noErrorHandler())
            .setVariable("authorities", constant("ROLE_SEARCH_${to.replace("direct:", "").uppercase()}"))
            .to("direct:auth")
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
            .setVariable("authorities", constant("ROLE_SEARCH_${to.replace("direct:", "").uppercase()}"))
            .to("direct:auth")
            .routeId("direct:${to}_api_search")
            .to(to)
            .marshal().json()
    }

    fun handleAccessDeniedException() {
        onException(AccessDeniedException::class.java)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpStatus.UNAUTHORIZED.value()))
    }

}