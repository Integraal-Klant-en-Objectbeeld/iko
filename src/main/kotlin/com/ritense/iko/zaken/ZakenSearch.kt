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
            .errorHandler(noErrorHandler())
            .to(OpenZaakZaakRead.URI)
            .setHeader("uuid", header("zaakId"))
            .unmarshal().json()
    }
}