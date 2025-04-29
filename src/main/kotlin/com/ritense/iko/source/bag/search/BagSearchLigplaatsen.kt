package com.ritense.iko.source.bag.search

class BagSearchLigplaatsen : BagSearch() {
    companion object {
        val URI = "direct:bagSearchLigplaatsen"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(URI, "ligplaatsIdentificatie", "identificatie")

        searchRoute(
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