package com.ritense.iko.connectors.bag.endpoints

class BagEndpointPanden : BagEndpoint() {
    companion object {
        val URI = "direct:bagSearchPanden"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(URI, "pandIdentificatie", "identificatie") { routeDefinition ->
            routeDefinition
                .setHeader("Accept-Crs", constant("epsg:28992"))
        }

        searchRoute(
            URI, "zoekPanden", listOf(
                "beschikbaarOp",
                "huidig",
                "page",
                "pageSize",
                "point",
                "bbox",
                "statusPand",
                "geconstateerd",
                "bouwjaar",
                "adresseerbaarObjectIdentificatie",
                "nummeraanduidingIdentificatie",
                "Content-Crs"
            )
        ) { routeDefinition ->
            routeDefinition
                .setHeader("Accept-Crs", constant("epsg:28992"))
        }

    }
}