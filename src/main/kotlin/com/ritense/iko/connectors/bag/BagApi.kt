package com.ritense.iko.connectors.bag

import org.apache.camel.builder.RouteBuilder

class BagApi : RouteBuilder() {

    companion object {
        val URI = "direct:bagApi"
    }

    override fun configure() {
        from(URI)
            .errorHandler(noErrorHandler())
            .setHeader("Accept", constant("application/hal+json"))
            .setHeader("X-Api-Key", simple("{{iko.sources.bag.apiKey}}"))
            .toD("bag:\${header.bagApiOperation}?throwExceptionOnFailure=false")
            .unmarshal().json()
    }

}