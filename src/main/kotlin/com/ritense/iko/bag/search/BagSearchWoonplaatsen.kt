package com.ritense.iko.bag.search

import org.springframework.stereotype.Component

@Component
class BagSearchWoonplaatsen : BagSearch() {
    companion object {
        val URI = "direct:bagSearchWoonplaatsen"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(URI, "woonplaatsIdentificatie", "identificatie")

        searchRoute(
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