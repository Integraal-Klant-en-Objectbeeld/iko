package com.ritense.iko.connectors.camel

import org.apache.camel.builder.RouteBuilder

class Connector : RouteBuilder() {
    override fun configure() {
        from(Iko.connector())
            .routeId("connector")
            .errorHandler(noErrorHandler())
            .toD(Iko.connector("\${variable.connector}"))
            .process { it }
    }
}