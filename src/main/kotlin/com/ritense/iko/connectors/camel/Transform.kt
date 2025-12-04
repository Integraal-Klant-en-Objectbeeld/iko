package com.ritense.iko.connectors.camel

import org.apache.camel.builder.RouteBuilder

class Transform : RouteBuilder() {
    override fun configure() {
        from(Iko.transform())
            .routeId("transform")
            .errorHandler(noErrorHandler())
            .choice()
            .`when` { ex -> ex.context.hasEndpoint(Iko.transform("${ex.getVariable("connector")}")) != null }
            .toD(Iko.transform("\${variable.connector}"))
            .end()
            .choice()
            .`when` { ex ->
                ex.context.hasEndpoint(
                    Iko.transform("${ex.getVariable("connector")}.${ex.getVariable("operation")}"),
                ) != null
            }.toD(Iko.transform("\${variable.connector}.\${variable.operation}"))
            .end()
    }
}