package com.ritense.iko.connectors.camel

import org.apache.camel.builder.RouteBuilder

class Connector : RouteBuilder() {
    override fun configure() {
        from(Iko.Companion.connector())
            .errorHandler(noErrorHandler())
            .toD(Iko.Companion.connector("\${variable.connector}"))
    }
}