package com.ritense.iko.connectors.openzaak

import org.apache.camel.builder.RouteBuilder

class OpenZaakApi : RouteBuilder() {
    companion object {
        val URI = "direct:openZaakApi"
    }
    override fun configure() {
        from(URI)
            .errorHandler(noErrorHandler())
            .bean(OpenZaakTokenService::class.java, "generateToken(*, {{iko.connectors.openzaak.clientId}}, {{iko.connectors.openzaak.secret}})")
            .toD("openZaak:\${header.openZaakApiOperation}")
            .unmarshal().json()
    }
}