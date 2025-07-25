package com.ritense.iko.connectors.bag.endpoints

class BagEndpointWoonplaatsen : BagEndpoint() {
    companion object {
        val URI = "direct:bagEndpointWoonplaatsen"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "woonplaatsIdentificatie", "identificatie")

        endpointRoute(
            URI, "zoekWoonplaatsen", listOf(
                "naam",
                "geldigOp",
                "beschikbaarOp",
                "huidig",
                "expand",
                "page",
                "pageSize",
                "point",
                "bbox",
                "Content-Crs"
            )
        ) { routeDefinition ->
            routeDefinition
                .setHeader("Accept-Crs", constant("epsg:28992"))
        }

    }
}