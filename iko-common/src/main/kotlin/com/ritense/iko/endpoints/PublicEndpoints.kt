package com.ritense.iko.endpoints

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException

abstract class PublicEndpoints : RouteBuilder() {

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

    fun idWithConfig(module: String, operation: String, to: String) {
        rest("/endpoints")
            .get("/${module}/{config}/${operation}/{id}")
            .to("direct:${to}_id")

        from("direct:${to}_id")
            .errorHandler(noErrorHandler())
            .setVariable("module", constant("${module}"))
            .setVariable("config", header("config"))
            .setVariable("operation", constant("${operation}"))
            .setVariable("authorities", simple("ROLE_ENDPOINT_\${header.module}_\${header.config}_\${header.operation}"))
            .to("direct:auth")
            .routeId("direct:${to}_api_id")
            .to(to)
            .marshal().json()
    }

    fun endpointWithConfig(module: String, operation: String, to: String) {
        rest("/endpoints")
            .get("/${module}/{config}/${operation}")
            .to("direct:${to}_endpoint")

        from("direct:${to}_endpoint")
            .errorHandler(noErrorHandler())
            .setVariable("module", header("module"))
            .setVariable("config", header("config"))
            .setVariable("operation", header("operation"))
            .setVariable("authorities", simple("ROLE_ENDPOINT_${module}_\${header.config}_${operation}"))
            .to("direct:auth")
            .routeId("direct:${to}_api_endpoint")
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