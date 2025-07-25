package com.ritense.iko.connectors.bag.endpoints

class BagEndpointAdressen : BagEndpoint() {
    companion object {
        val URI = "direct:bagEndpointAdressen"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "bevraagAdressenMetNumId", "nummeraanduidingIdentificatie") {
            it.setHeader("expand", constant("true"))
        }

        endpointRoute(
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