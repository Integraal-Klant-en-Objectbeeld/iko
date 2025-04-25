package com.ritense.iko.bag.search

import com.ritense.iko.bag.BagApi
import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

@Component
class BagSearchAdressen : BagSearch() {
    companion object {
        val URI = "direct:bagSearchAdressen"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(URI, "bevraagAdressenMetNumId", "nummeraanduidingIdentificatie")

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