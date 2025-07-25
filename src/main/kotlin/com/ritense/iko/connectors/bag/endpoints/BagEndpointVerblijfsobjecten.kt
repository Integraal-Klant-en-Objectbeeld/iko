package com.ritense.iko.connectors.bag.endpoints

class BagEndpointVerblijfsobjecten : BagEndpoint() {
    companion object {
        val URI = "direct:bagEndpointVerblijfsobjecten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "verblijfsobjectIdentificatie", "identificatie")

        endpointRoute(
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