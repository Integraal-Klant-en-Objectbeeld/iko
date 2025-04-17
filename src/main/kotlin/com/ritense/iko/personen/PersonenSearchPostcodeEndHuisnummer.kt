package com.ritense.iko.personen

import com.ritense.iko.personen.PersonenValidations.Companion.VALID_HUISNUMMER
import com.ritense.iko.personen.PersonenValidations.Companion.VALID_POSTCODE
import org.apache.camel.builder.RouteBuilder

class PersonenSearchPostcodeEndHuisnummer : RouteBuilder() {

    companion object {
        val URI = "direct:personenSearch_postcodeEnHuisnummer"
    }

    override fun configure() {
        from(URI)
            .routeId(this::class.java.canonicalName)
            .to(VALID_POSTCODE)
            .to(VALID_HUISNUMMER)
            .setBody { exchange ->
                val eIn = exchange.getIn()
                mapOf(
                    "type" to "ZoekMetPostcodeEnHuisnummer",
                    "huisnummer" to eIn.getHeader("huisnummer", Long::class.java),
                    "postcode" to eIn.getHeader("postcode"),
                    "fields" to listOf("burgerservicenummer", "naam")
                )
            }
            .to(BrpPersonenSearch.URI)
            .transform(jq(".personen"))
            .unmarshal().json()
    }
}