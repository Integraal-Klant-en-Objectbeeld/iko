package com.ritense.iko.connectors

import com.ritense.iko.connectors.Iko.Companion.transform
import org.apache.camel.builder.RouteBuilder

class Transform : RouteBuilder() {
    override fun configure() {
        from(transform())
            .errorHandler(noErrorHandler())
            .choice()
            .`when` { ex -> ex.context.hasEndpoint(transform("${ex.getVariable("connector")}")) != null }
            .toD(transform("\${variable.connector}"))
            .end()
            .choice()
            .`when` { ex -> ex.context.hasEndpoint(transform("${ex.getVariable("connector")}.${ex.getVariable("operation")}")) != null }
            .toD(transform("\${variable.connector}.\${variable.operation}"))
            .end()
    }
}