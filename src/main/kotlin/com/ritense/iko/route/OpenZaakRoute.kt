package com.ritense.iko.route

import com.ritense.iko.openzaak.TokenGeneratorService
import com.ritense.iko.processor.ZaakResponseProcessor
import org.apache.camel.builder.RouteBuilder

class OpenZaakRoute : RouteBuilder() {
    override fun configure() {
        val generatedToken = TokenGeneratorService().generateToken() // TODO move to config

        from("direct:openZaak")
            .setHeader("Accept-Crs", constant("EPSG:4326"))
            .setHeader("Content-Crs", constant("EPSG:4326"))
            .setHeader("Authorization", constant("Bearer $generatedToken"))
            .setHeader("uuid", constant("68d41e2e-336a-4b4c-82b0-8530bb70bfbc")) // TODO refactor to search
            .to("openZaak:zaak_read")
            .unmarshal().json()
            .process(ZaakResponseProcessor)
    }
}