package com.ritense.iko.zaken

import com.ritense.iko.openzaak.TokenGeneratorService
import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

@Component
class OpenZaakZaakList : RouteBuilder() {

    companion object {
        val URI = "direct:openZaak_zaakList"
    }

    override fun configure() {
        val generatedToken = TokenGeneratorService().generateToken()

        from(URI)
            .routeId(this::class.java.canonicalName)
            .setHeader("Accept-Crs", constant("EPSG:4326"))
            .setHeader("Authorization", constant("Bearer $generatedToken"))
            .to("openZaak:zaak_list")
    }
}