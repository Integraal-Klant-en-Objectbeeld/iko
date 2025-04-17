package com.ritense.iko.personen

import com.ritense.iko.personen.PersonenValidations.Companion.VALID_BSN
import org.apache.camel.builder.RouteBuilder

class PersonenSearchBsn : RouteBuilder() {

    companion object {
        val URI = "direct:personenSearch_bsn"
    }

    override fun configure() {
        from(URI)
            .routeId(this::class.java.canonicalName)
            .to(VALID_BSN)
            .setBody { exchange ->
                mapOf(
                    "type" to "RaadpleegMetBurgerservicenummer",
                    "burgerservicenummer" to listOf(exchange.getIn().getHeader("bsn", String::class.java)),
                    "fields" to listOf("burgerservicenummer", "naam")
                )
            }
            .to(BrpPersonenSearch.URI)
            .transform(jq(".personen"))
            .unmarshal().json()
    }
}