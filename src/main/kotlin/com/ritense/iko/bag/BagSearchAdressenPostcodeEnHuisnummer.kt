package com.ritense.iko.bag

import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

@Component
class BagSearchAdressenPostcodeEnHuisnummer : RouteBuilder() {
    companion object {
        val URI = "direct:bagSearchAdressen_postcodeEnHuisnummer"
    }

    override fun configure() {
        from(URI)
            .removeHeaders("*", "postcode", "huisnummer")
            .setHeader("bagApiOperation", constant("bevraagAdressen"))
            .setHeader("postcode", header("postcode"))
            .setHeader("huisnummer", header("huisnummer"))
            .setHeader("nummeraanduidingIdentificatie", header("id"))
            .to(BagApi.URI)
    }
}