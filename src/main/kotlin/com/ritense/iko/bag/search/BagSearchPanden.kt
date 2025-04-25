package com.ritense.iko.bag.search

import org.springframework.stereotype.Component

@Component
class BagSearchPanden : BagSearch() {
    companion object {
        val URI = "direct:bagSearchPanden"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(URI, "pandIdentificatie", "identificatie")

        searchRoute(
            URI, "zoekPanden", listOf(
                "beschikbaarOp",
                "huidig",
                "page",
                "pageSize",
                "point",
                "bbox",
                "statusPand",
                "geconstateerd",
                "bouwjaar",
                "adresseerbaarObjectIdentificatie",
                "nummeraanduidingIdentificatie",
                "Content-Crs"
            )
        ) { routeDefinition ->
            routeDefinition
                .setHeader("Accept-Crs", constant("epsg:28992"))
        }

    }
}