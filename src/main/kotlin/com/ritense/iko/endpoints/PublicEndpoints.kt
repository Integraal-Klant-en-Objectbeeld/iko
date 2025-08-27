package com.ritense.iko.endpoints

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.springframework.http.HttpStatus

abstract class PublicEndpointss : RouteBuilder() {

    fun id(uri: String, to: String) {
        rest("/endpoints$uri")
            .get("/{id}")
            .to("direct:${to}_id")

        from("direct:${to}_id")
            .errorHandler(noErrorHandler())
            .setVariable("authorities", constant("ROLE_ENDPOINT_${to.replace("direct:", "").uppercase()}"))
            .to("direct:auth")
            .routeId("direct:${to}_api_id")
            .to(to)
            .marshal().json()
    }

    fun endpoint(uri: String, to: String) {
        rest("/endpoints$uri")
            .get()
            .to("direct:${to}_endpoint")

        from("direct:${to}_endpoint")
            .errorHandler(noErrorHandler())
            .setVariable("authorities", constant("ROLE_ENDPOINT_${to.replace("direct:", "").uppercase()}"))
            .to("direct:auth")
            .routeId("direct:${to}_api_endpoint")
            .to(to)
            .marshal().json()
    }

    fun handleAccessDeniedException() {
        onException(AccessDeniedException::class.java)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpStatus.UNAUTHORIZED.value()))
    }

}