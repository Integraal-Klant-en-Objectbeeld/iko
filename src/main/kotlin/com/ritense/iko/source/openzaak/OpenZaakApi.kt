package com.ritense.iko.source.openzaak

import org.apache.camel.builder.RouteBuilder

class OpenZaakApi : RouteBuilder() {
    companion object {
        val URI = "direct:openZaakApi"
    }
    override fun configure() {
        from(URI)
            .errorHandler(noErrorHandler())
            .bean(OpenZaakTokenService::class.java, "generateToken(*, {{iko.sources.openzaak.clientId}}, {{iko.sources.openzaak.secret}})")
            .toD("openZaak:\${header.openZaakApiOperation}")
            .unmarshal().json()
    }
}