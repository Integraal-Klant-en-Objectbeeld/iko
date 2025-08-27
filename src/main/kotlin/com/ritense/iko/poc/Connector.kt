package com.ritense.iko.poc

import com.ritense.iko.poc.Iko.Companion.connector
import org.apache.camel.builder.RouteBuilder

class Connector : RouteBuilder() {
    override fun configure() {
        from(connector())
            .toD(connector("\${variable.connector}"))
    }
}