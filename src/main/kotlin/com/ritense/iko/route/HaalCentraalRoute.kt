package com.ritense.iko.route

import com.ritense.iko.processor.HaalcentraalResponseProcessor
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder

class HaalCentraalRoute : RouteBuilder() {
    override fun configure() {
        from("direct:haalcentraal")
            .setHeader("Accept", constant("application/json"))
            .setHeader(Exchange.HTTP_URI).header("url")
            .removeHeader(Exchange.HTTP_PATH)
            .removeHeader(Exchange.HTTP_URI)
            .setBody {
                """
                {
                    "type": "RaadpleegMetBurgerservicenummer",
                    "burgerservicenummer": ["999990755"],
                    "fields": ["burgerservicenummer", "naam", "geslacht"]
                }
                """.trimIndent()
            }
            .to("haalcentraal:Personen")
            .log("Body before JQ: \${body}")
            .transform().jq(
                """
                {
                  burgerservicenummer: .personen[0].burgerservicenummer,
                  geslacht: {
                    omschrijving: .personen[0].geslacht.omschrijving
                  },
                  naam: {
                    aanduidingNaamgebruik: {
                      omschrijving: .personen[0].naam.aanduidingNaamgebruik.omschrijving
                    },
                    voornamen: .personen[0].naam.voornamen,
                    geslachtsnaam: .personen[0].naam.geslachtsnaam,
                    voorletters: .personen[0].naam.voorletters,
                    volledigeNaam: .personen[0].naam.volledigeNaam
                  }
                }
                """.trimIndent()
            )
            .log("After JQ before marshal: \${body}")
            .unmarshal().json()
            .process(HaalcentraalResponseProcessor)
    }
}