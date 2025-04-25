package com.ritense.iko.bag.search

import com.ritense.iko.bag.BagApi
import org.springframework.stereotype.Component

@Component
class BagSearchBronhouders : BagSearch() {
    companion object {
        val URI = "direct:bagSearchBronhouders"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(URI, "bevragenBronhouder", "identificatie")

        searchRoute(
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