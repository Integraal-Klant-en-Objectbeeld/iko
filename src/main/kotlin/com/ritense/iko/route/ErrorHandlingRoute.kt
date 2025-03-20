package com.ritense.iko.route

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder

class ErrorHandlingRoute : RouteBuilder() {
    override fun configure() {
        from("direct:errorHandle")
            .process { exchange ->
                val failedEndpoint = exchange.getProperty(Exchange.FAILURE_ENDPOINT, String::class.java) ?: "unknown"
                val failedService = failedEndpoint.substringAfter("direct:")
                val errorResponse = mapOf(failedService to "Failed")
                exchange.getIn().body = errorResponse
            }
    }
}