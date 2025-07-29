package com.ritense.iko.connectors.bag.endpoints

class BagEndpointBronhouders : BagEndpoint() {
    companion object {
        val URI = "direct:bagEndpointBronhouders"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "bevragenBronhouder", "identificatie")

        endpointRoute(
            URI, "zoekBronhouder", listOf(
                 "woonplaatsIdentificatie",
                "objectIdentificatie",
                "geldigOp",
                "beschikbaarOp"
            )
        ) { routeDefinition ->
            routeDefinition.setHeader("Accept-Crs", constant("epsg:28992"))
        }
    }
}