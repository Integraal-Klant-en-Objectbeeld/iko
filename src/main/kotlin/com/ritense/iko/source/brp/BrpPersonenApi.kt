package com.ritense.iko.source.brp

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder

class BrpPersonenApi : RouteBuilder() {

    companion object {
        val URI = "direct:brpPersonenApi"
    }

    override fun configure() {
        from(URI)
            .errorHandler(noErrorHandler())
            .setHeader("Accept", constant("application/json"))
            .setHeader(Exchange.HTTP_URI).header("url")
            .removeHeader(Exchange.HTTP_PATH)
            .removeHeader(Exchange.HTTP_URI)
            .to("brp:Personen")
            .unmarshal().json()
    }
}