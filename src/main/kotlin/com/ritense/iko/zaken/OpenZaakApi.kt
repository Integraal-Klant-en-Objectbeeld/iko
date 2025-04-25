package com.ritense.iko.zaken

import com.ritense.iko.openzaak.TokenGeneratorService
import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

@Component
class OpenZaakApi : RouteBuilder() {
    companion object {
        val URI = "direct:openZaakApi"
    }
    override fun configure() {
        val generatedToken = TokenGeneratorService().generateToken()

        from(URI)
            .setHeader("Authorization", constant("Bearer $generatedToken"))
            .toD("openZaak:\${header.openZaakApiOperation}")
            .unmarshal().json()
    }
}