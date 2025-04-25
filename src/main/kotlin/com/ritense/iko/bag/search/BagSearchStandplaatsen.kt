package com.ritense.iko.bag.search

import org.springframework.stereotype.Component

@Component
class BagSearchStandplaatsen : BagSearch() {
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