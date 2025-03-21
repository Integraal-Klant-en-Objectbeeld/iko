package com.ritense.iko.route.profiel

import com.ritense.iko.openzaak.TokenGeneratorService
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder

class ZakenOpvragenRoute : RouteBuilder() {
    override fun configure() {
        val generatedToken = TokenGeneratorService().generateToken() // TODO move to config
        from("direct:zakenOpvragen")
            //.log("BSN received: \${header.bsn}")
            .setHeader("Accept-Crs", constant("EPSG:4326"))
            .setHeader("Content-Crs", constant("EPSG:4326"))
            .setHeader("Authorization", constant("Bearer $generatedToken"))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setHeader(Exchange.HTTP_URI).header("url")
            .removeHeader(Exchange.HTTP_PATH)
            .removeHeader(Exchange.HTTP_URI)
            .setHeader(BSN_PROPERTY, simple("\${header.bsn}"))
            .setHeader("expand", constant("zaaktype,status,status.statustype")) // TODO status.statustype
            .to("openZaak:zaak_list")
            //.log("Body before marshal: \${body}")
            .convertBodyTo(String::class.java)
    }

    companion object {
        private const val BSN_PROPERTY = "rol__betrokkeneIdentificatie__natuurlijkPersoon__inpBsn"
    }
}


// "deelzaken"
// "deelzaken.resultaat"
// "deelzaken.resultaat.resultaattype"
// "deelzaken.rollen"
// "deelzaken.rollen.roltype"
// "deelzaken.status"
// "deelzaken.status.statustype"
// "deelzaken.zaakinformatieobjecten"
// "deelzaken.zaakobjecten"
// "deelzaken.zaaktype"
// "eigenschappen"
// "eigenschappen.eigenschap"
// "hoofdzaak"
// "hoofdzaak.resultaat"
// "hoofdzaak.resultaat.resultaattype"
// "hoofdzaak.rollen"
// "hoofdzaak.rollen.roltype"
// "hoofdzaak.status"
// "hoofdzaak.status.statustype"
// "hoofdzaak.zaakinformatieobjecten"
// "hoofdzaak.zaakobjecten"
// "hoofdzaak.zaaktype"
// "resultaat"
// "resultaat.resultaattype"
// "rollen"
// "rollen.roltype"
// "status"
// "status.statustype"
// "zaakinformatieobjecten"
// "zaakobjecten"
// "zaaktype"
