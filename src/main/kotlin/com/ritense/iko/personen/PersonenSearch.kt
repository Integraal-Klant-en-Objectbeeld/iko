package com.ritense.iko.personen

import com.ritense.iko.personen.PersonenValidations.Companion.VALID_BSN
import org.apache.camel.builder.RouteBuilder

class PersonenSearch : RouteBuilder() {

    companion object {
        val URI = "direct:personenSearch"
    }

    override fun configure() {
        from(URI)
            .setHeader("bsn", header("id"))
            .removeHeaders("*", "bsn")
            .errorHandler(noErrorHandler())
            .routeId(this::class.java.canonicalName)
            .to(VALID_BSN)
            .setBody { exchange ->
                mapOf(
                    "type" to "RaadpleegMetBurgerservicenummer",
                    "burgerservicenummer" to listOf(exchange.getIn().getHeader("bsn")),
                    "fields" to listOf(
                        "aNummer",
                        "adressering",
                        "burgerservicenummer",
                        "datumEersteInschrijvingGBA",
                        "datumInschrijvingInGemeente",
                        "geboorte",
                        "gemeenteVanInschrijving",
                        "geslacht",
                        "gezag",
                        "immigratie",
                        "indicatieCurateleRegister",
                        "kinderen",
                        "leeftijd",
                        "naam",
                        "nationaliteiten",
                        "ouders",
                        "overlijden",
                        "partners",
                        "uitsluitingKiesrecht",
                        "verblijfplaats",
                        "verblijfstitel",
                        "verblijfplaatsBinnenland",
                        "adresseringBinnenland"
                    )
                )
            }
            .to(BrpPersonenSearch.URI)
            .transform(jq(".personen[0]"))
            .unmarshal().json()
    }
}