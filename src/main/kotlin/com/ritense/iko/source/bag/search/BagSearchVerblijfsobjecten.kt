package com.ritense.iko.source.bag.search

class BagSearchVerblijfsobjecten : BagSearch() {
    companion object {
        val URI = "direct:bagSearchVerblijfsobjecten"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(URI, "verblijfsobjectIdentificatie", "identificatie")

        searchRoute(
            URI, "zoekVerblijfsobjecten", listOf(
                "pandIdentificatie",
                "geldigOp",
                "beschikbaarOp",
                "huidig",
                "page",
                "pageSize",
                "expand",
                "oppervlakte",
                "bbox",
                "gebruiksdoelen",
                "geconstateerd",
                "Content-Crs"
            )
        ) { routeDefinition ->
            routeDefinition
                .setHeader("Accept-Crs", constant("epsg:28992"))
        }

    }
}