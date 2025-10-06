package com.ritense.iko.connectors

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException

class Endpoint() : RouteBuilder() {
    override fun configure() {
        onException(AccessDeniedException::class.java)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpStatus.UNAUTHORIZED.value()))

        rest("/endpoints")
            .get("/{iko_connector}/{iko_config}/{iko_operation}")
            .to(Iko.iko("rest:endpoint"))
            .get("/{iko_connector}/{iko_config}/{iko_operation}/{id}")
            .to(Iko.iko("rest:endpoint.id"))

        from(Iko.iko("rest:endpoint"))
            .errorHandler(noErrorHandler())
            .setVariable("connector", header("iko_connector"))
            .setVariable("config", header("iko_config"))
            .setVariable("operation", header("iko_operation"))
            .removeHeaders("iko_*")
            .to(Iko.endpoint("validate"))
            .to(Iko.endpoint("auth"))
            .to(Iko.iko("config"))
            .to(Iko.transform())
            .to(Iko.connector())
            .marshal().json()

        from(Iko.iko("rest:endpoint.id"))
            .errorHandler(noErrorHandler())
            .setVariable("connector", header("iko_connector"))
            .setVariable("config", header("iko_config"))
            .setVariable("operation", header("iko_operation"))
            .removeHeaders("iko_*")
            .to(Iko.endpoint("validate"))
            .to(Iko.endpoint("auth"))
            .to(Iko.iko("config"))
            .to(Iko.transform())
            .toD(Iko.connector())
            .marshal().json()


    }
}