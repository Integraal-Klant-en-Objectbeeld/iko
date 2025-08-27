package com.ritense.iko.endpoints

import org.apache.camel.builder.RouteBuilder

class RestApi(val uri: String) : RouteBuilder() {
    override fun configure() {
        from(uri)
            .errorHandler(noErrorHandler())
            .setHeader("Accept", constant("application/json"))
            .process { exchange ->
                val module = exchange.getVariable("module") as String
                val config = exchange.getVariable("config") as String
                val operation = exchange.getVariable("operation") as String
            }
            .toD("direct:auth:\${variable.module}.\${variable.config}")
            .toD("direct:headers:\${variable.module}.\${variable.config}.\${variable.operation}")
            .toD("\${variable.module}.\${variable.config}:\${header.operation}?throwExceptionOnFailure=false")
            .unmarshal().json()

        from("direct:auth:openklant.\${variable.config}")
            .setHeader("X-Api-Key", simple("{{iko.connectors.openklant.apiKey}}"))
    }
}