package com.ritense.iko.connectors.bag.endpoints

class BagEndpointAdresseerbareObjecten : BagEndpoint() {

    companion object {
        val URI = "direct:bagSearchAdresseerbareObjecten"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(URI, "bevragenAdresseerbaarObject", "adresseerbaarObjectIdentificatie") { routeDefinition ->
            routeDefinition.setHeader("Accept-Crs", constant("epsg:28992"))
        }

        searchRoute(
            URI, "zoekAdresseerbareObjecten", listOf(
                "adresseerbaarObjectIdentificatie",
                "geldigOp",
                "beschikbaarOp",
                "expand",
                "huidig",
                "page",
                "pageSize",
                "bbox",
                "geconstateerd",
                "oppervlakte",
                "gebruiksdoelen",
                "type",
                "pandIdentificaties",
                "Content-Crs"
            )
        ) { routeDefinition ->
            routeDefinition.setHeader("Accept-Crs", constant("epsg:28992"))
        }
    }
}