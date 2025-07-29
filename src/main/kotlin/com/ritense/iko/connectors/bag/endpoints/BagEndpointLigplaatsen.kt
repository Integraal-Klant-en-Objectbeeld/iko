package com.ritense.iko.connectors.bag.endpoints

class BagEndpointLigplaatsen : BagEndpoint() {
    companion object {
        val URI = "direct:bagEndpointLigplaatsen"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "ligplaatsIdentificatie", "identificatie") { routeDefinition ->
            routeDefinition
                .setHeader("Accept-Crs", constant("epsg:28992"))
        }

        endpointRoute(
            URI, "zoekLigplaatsen", listOf(
                "point",
                "bbox",
                "geldigOp",
                "beschikbaarOp",
                "Content-Crs"
            )
        ) { routeDefinition ->
            routeDefinition
                .setHeader("Accept-Crs", constant("epsg:28992"))
                .setHeader("Content-Crs", constant("epsg:28992"))
        }
    }
}