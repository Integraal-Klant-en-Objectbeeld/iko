package com.ritense.iko.personen

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder

class BrpPersonenSearch : RouteBuilder() {

    companion object {
        val URI = "direct:brp_personen"
    }

    override fun configure() {
        from(URI)
            .errorHandler(noErrorHandler())
            .setHeader("Accept", constant("application/json"))
            .setHeader(Exchange.HTTP_URI).header("url")
            .removeHeader(Exchange.HTTP_PATH)
            .removeHeader(Exchange.HTTP_URI)
            .marshal().json()
            .to("brp:Personen")
    }
}