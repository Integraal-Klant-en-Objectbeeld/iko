package com.ritense.iko.zaken

import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

@Component
class ZakenSearch : RouteBuilder() {
    companion object {
        val URI = "direct:zakenSearch"
    }

    override fun configure() {
        from(URI)
            .routeId(this::class.java.canonicalName)
            .setHeader("uuid", header("id"))
            .errorHandler(noErrorHandler())
            .to(OpenZaakZaakRead.URI)
            .unmarshal().json()
    }
}