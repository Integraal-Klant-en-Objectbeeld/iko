package com.ritense.iko.connectors.openklant

import org.apache.camel.builder.RouteBuilder

class OpenKlantApi : RouteBuilder() {

    companion object {
        val URI = "direct:openKlantApi"
    }

    override fun configure() {
        from(URI)
            .errorHandler(noErrorHandler())
            .setHeader("Accept", constant("application/json"))
            // Add authentication headers if needed
            // .setHeader("Authorization", simple("Bearer {{iko.connectors.openklant.token}}"))
            .toD("openklant:\${header.openKlantOperation}?throwExceptionOnFailure=false")
            .unmarshal().json()
    }
}