package com.ritense.iko.connectors.bag.endpoints

class BagEndpointStandplaatsen : BagEndpoint() {
    companion object {
        val URI = "direct:bagSearchStandplaatsen"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(URI, "standplaatsIdentificatie", "identificatie")

        searchRoute(
            URI, "zoekStandplaatsen", listOf(
                "geldigOp",
                "beschikbaarOp",
                "huidig",
                "page",
                "pageSize",
                "expand",
                "point",
                "bbox",
                "statusPand",
                "geconstateerd",
                "Content-Crs"
            )
        ) { routeDefinition ->
            routeDefinition
                .setHeader("Accept-Crs", constant("epsg:28992"))
        }

    }
}