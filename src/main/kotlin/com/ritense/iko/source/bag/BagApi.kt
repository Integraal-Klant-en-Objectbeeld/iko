package com.ritense.iko.source.bag

import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

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