package com.ritense.iko.connectors.bag.endpoints

class BagEndpointAdressen : BagEndpoint() {
    companion object {
        val URI = "direct:bagSearchAdressen"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(URI, "bevraagAdressenMetNumId", "nummeraanduidingIdentificatie") {
            it.setHeader("expand", constant("true"))
        }

        searchRoute(
            URI, "bevraagAdressen", listOf(
                "zoekresultaatIdentificatie",
                "postcode",
                "huisnummer",
                "huisnummertoevoeging",
                "huisletter",
                "exacteMatch",
                "adresseerbareObjectIdentificatie",
                "woonplaatsNaam",
                "openbareRuimteNaam",
                "pandIdentificatie",
                "expand",
                "page",
                "pageSize",
                "q",
                "inclusiefEindStatus",
                "openbareRuimteIdentificatie"
            )
        ) { routeDefinition ->
            routeDefinition.setHeader("Accept-Crs", constant("epsg:28992"))
        }
    }
}