package com.ritense.iko.source.brp.search

import com.ritense.iko.source.brp.BrpPersonenApi
import com.ritense.iko.source.brp.PersonenValidations.Companion.VALID_BSN
import org.apache.camel.builder.RouteBuilder

class BrpPersonenSearch : RouteBuilder() {

    companion object {
        val URI = "direct:brpPersonenSearch"
    }

    override fun configure() {
        from(URI)
            .routeId(URI)
            .choice()
            .`when`(simple("\${header.id} != null"))
            .to("${URI}_id")
            .otherwise()
            .to("${URI}_search")

        from("${URI}_id")
            .routeId("${URI}_id")
            .removeHeaders("*", "id")
            .errorHandler(noErrorHandler())
            .setHeader("bsn", header("id"))
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
                ).filter { it.value != null }
            }
            .marshal().json()
            .to(BrpPersonenApi.URI)
            .transform(jq(".personen[0]"))

        from("${URI}_search")
            .routeId("${URI}_search")
            .errorHandler(noErrorHandler())
            .choice()
            .`when`(simple("\${header.type} == 'ZoekMetPostcodeEnHuisnummer'"))
            .to("${URI}_postcodeEnHuisnummer")
            .`when`(simple("\${header.type} == 'ZoekMetGeslachtsnaamEnGeboortedatum'"))
            .to("${URI}_geslachtsnaamEnGeboortedatum")
            .`when`(simple("\${header.type} == 'ZoekMetNaamEnGemeenteVanInschrijving'"))
            .to("${URI}_naamEnGemeenteVanInschrijving")
            .`when`(simple("\${header.type} == 'ZoekMetStraatHuisnummerEnGemeenteVanInschrijving'"))
            .to("${URI}_straatHuisnummerEnGemeenteVanInschrijving")
            .`when`(simple("\${header.type} == 'ZoekMetNummeraanduidingIdentificatie'"))
            .to("${URI}_nummeraanduidingIdentificatie")
            .`when`(simple("\${header.type} == 'ZoekMetAdresseerbaarObjectIdentificatie'"))
            .to("${URI}_adresseerbaarObjectIdentificatie")
            .otherwise()
            .to("${URI}_burgerservicenummer")
            .end()
            .removeHeaders(
                "*",
            )
            .to(BrpPersonenApi.URI)

        from("${URI}_adresseerbaarObjectIdentificatie")
            .setBody { exchange ->
                mapOf(
                    "type" to "ZoekMetAdresseerbaarObjectIdentificatie",
                    "fields" to listOf(
                        "burgerservicenummer",
                        "naam",
                    ),
                    "gemeenteVanInschrijving" to exchange.getIn().getHeader("gemeenteVanInschrijving"),
                    "inclusiefOverledenPersonen" to exchange.getIn().getHeader("inclusiefOverledenPersonen"),
                    "adresseerbaarObjectIdentificatie" to exchange.getIn()
                        .getHeader("adresseerbaarObjectIdentificatie"),
                ).filter { it.value != null }
            }
            .marshal().json()

        from("${URI}_nummeraanduidingIdentificatie")
            .setBody { exchange ->
                mapOf(
                    "type" to "ZoekMetNummeraanduidingIdentificatie",
                    "fields" to listOf(
                        "burgerservicenummer",
                        "naam",
                    ),
                    "gemeenteVanInschrijving" to exchange.getIn().getHeader("gemeenteVanInschrijving"),
                    "inclusiefOverledenPersonen" to exchange.getIn().getHeader("inclusiefOverledenPersonen"),
                    "nummeraanduidingIdentificatie" to exchange.getIn().getHeader("nummeraanduidingIdentificatie"),
                ).filter { it.value != null }
            }
            .marshal().json()

        from("${URI}_straatHuisnummerEnGemeenteVanInschrijving")
            .setBody { exchange ->
                mapOf(
                    "type" to "ZoekMetStraatHuisnummerEnGemeenteVanInschrijving",
                    "fields" to listOf(
                        "burgerservicenummer",
                        "naam",
                    ),
                    "gemeenteVanInschrijving" to exchange.getIn().getHeader("gemeenteVanInschrijving"),
                    "inclusiefOverledenPersonen" to exchange.getIn().getHeader("inclusiefOverledenPersonen"),
                    "huisletter" to exchange.getIn().getHeader("huisletter"),
                    "huisnummer" to exchange.getIn().getHeader("huisnummer"),
                    "huisnummertoevoeging" to exchange.getIn().getHeader("huisnummertoevoeging"),
                    "straat" to exchange.getIn().getHeader("straat"),
                ).filter { it.value != null }
            }
            .marshal().json()

        from("${URI}_naamEnGemeenteVanInschrijving")
            .setBody { exchange ->
                mapOf(
                    "type" to "ZoekMetNaamEnGemeenteVanInschrijving",
                    "fields" to listOf(
                        "burgerservicenummer",
                        "naam",
                    ),
                    "gemeenteVanInschrijving" to exchange.getIn().getHeader("gemeenteVanInschrijving"),
                    "inclusiefOverledenPersonen" to exchange.getIn().getHeader("inclusiefOverledenPersonen"),
                    "geslachtsnaam" to exchange.getIn().getHeader("geslachtsnaam"),
                    "geslacht" to exchange.getIn().getHeader("geslacht"),
                    "voorvoegsel" to exchange.getIn().getHeader("voorvoegsel"),
                    "voornamen" to exchange.getIn().getHeader("voornamen"),
                ).filter { it.value != null }
            }
            .marshal().json()

        from("${URI}_geslachtsnaamEnGeboortedatum")
            .setBody { exchange ->
                mapOf(
                    "type" to "ZoekMetGeslachtsnaamEnGeboortedatum",
                    "fields" to listOf(
                        "burgerservicenummer",
                        "naam",
                    ),
                    "gemeenteVanInschrijving" to exchange.getIn().getHeader("gemeenteVanInschrijving"),
                    "inclusiefOverledenPersonen" to exchange.getIn().getHeader("inclusiefOverledenPersonen"),
                    "geboortedatum" to exchange.getIn().getHeader("geboortedatum"),
                    "geslachtsnaam" to exchange.getIn().getHeader("geslachtsnaam"),
                    "geslacht" to exchange.getIn().getHeader("geslacht"),
                    "voorvoegsel" to exchange.getIn().getHeader("voorvoegsel"),
                    "voornamen" to exchange.getIn().getHeader("voornamen"),
                ).filter { it.value != null }
            }
            .marshal().json()

        from("${URI}_postcodeEnHuisnummer")
            .setBody { exchange ->
                mapOf(
                    "type" to "ZoekMetPostcodeEnHuisnummer",
                    "gemeenteVanInschrijving" to exchange.getIn().getHeader("gemeenteVanInschrijving"),
                    "inclusiefOverledenPersonen" to exchange.getIn().getHeader("inclusiefOverledenPersonen"),
                    "huisletter" to exchange.getIn().getHeader("huisletter"),
                    "huisnummer" to exchange.getIn().getHeader("huisnummer", Long::class.java),
                    "huisnummertoevoeging" to exchange.getIn().getHeader("huisnummertoevoeging"),
                    "postcode" to exchange.getIn().getHeader("postcode"),
                    "geboortedatum" to exchange.getIn().getHeader("geboortedatum"),
                    "geslachtsnaam" to exchange.getIn().getHeader("geslachtsnaam"),
                    "fields" to listOf(
                        "burgerservicenummer",
                        "naam",
                    )
                ).filter { it.value != null }
            }
            .marshal().json()
            .log("\${body}")


        from("${URI}_burgerservicenummer")
            .log("\${body}")
            .setBody { exchange ->
                mapOf(
                    "type" to "RaadpleegMetBurgerservicenummer",
                    "burgerservicenummer" to listOf(exchange.getIn().getHeader("bsn", String::class.java)),
                    "fields" to listOf("burgerservicenummer", "naam")
                ).filter { it.value != null }
            }
            .log("\${body}")
            .marshal().json()
            .log("\${body}")
    }
}