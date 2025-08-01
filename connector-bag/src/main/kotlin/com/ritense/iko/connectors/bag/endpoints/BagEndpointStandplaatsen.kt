package com.ritense.iko.connectors.bag.endpoints

class BagEndpointStandplaatsen : BagEndpoint() {
    companion object {
        val URI = "direct:bagEndpointStandplaatsen"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "standplaatsIdentificatie", "identificatie")

        endpointRoute(
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