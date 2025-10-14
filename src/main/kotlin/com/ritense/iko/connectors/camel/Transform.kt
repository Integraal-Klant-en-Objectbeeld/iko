package com.ritense.iko.connectors.camel

import org.apache.camel.builder.RouteBuilder

class Transform : RouteBuilder() {
    override fun configure() {
        from(Iko.Companion.transform())
            .errorHandler(noErrorHandler())
            .choice()
            .`when` { ex -> ex.context.hasEndpoint(Iko.Companion.transform("${ex.getVariable("connector")}")) != null }
            .toD(Iko.Companion.transform("\${variable.connector}"))
            .end()
            .choice()
            .`when` { ex -> ex.context.hasEndpoint(
                Iko.Companion.transform(
                    "${ex.getVariable("connector")}.${
                        ex.getVariable(
                            "operation"
                        )
                    }"
                )
            ) != null }
            .toD(Iko.Companion.transform("\${variable.connector}.\${variable.operation}"))
            .end()
    }
}