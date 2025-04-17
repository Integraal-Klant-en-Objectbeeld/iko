package com.ritense.iko.zaken

import com.ritense.iko.personen.PersonenValidations.Companion.VALID_BSN
import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

@Component
class ZakenSearchBsn : RouteBuilder() {
    companion object {
        val URI = "direct:zakenSearch_bsn"
    }

    override fun configure() {
        from(URI)
            .routeId(this::class.java.canonicalName)
            .errorHandler(noErrorHandler())
            .setHeader("rol__betrokkeneIdentificatie__natuurlijkPersoon__inpBsn", header("bsn"))
            .to(OpenZaakZaakList.URI)
            .unmarshal().json()

    }
}
