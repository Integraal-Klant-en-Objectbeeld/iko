package com.ritense.iko.connectors

import com.ritense.iko.connectors.Iko.Companion.connector
import org.apache.camel.builder.RouteBuilder

class Connector : RouteBuilder() {
    override fun configure() {
        from(connector())
            .errorHandler(noErrorHandler())
            .toD(connector("\${variable.connector}"))
    }
}