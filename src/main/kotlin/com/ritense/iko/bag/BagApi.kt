package com.ritense.iko.bag

import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

@Component
class BagApi : RouteBuilder() {

    companion object {
        val URI = "direct:bagApi"
    }

    override fun configure() {
        from(URI)
            .errorHandler(noErrorHandler())
            .setHeader("Accept", constant("application/hal+json"))
            .setHeader("X-Api-Key", constant("<API_TOKEN>"))
            .toD("bag:\${header.bagApiOperation}")
            .unmarshal().json()
    }

}