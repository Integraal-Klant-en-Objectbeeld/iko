package com.ritense.iko.bag.search

import org.springframework.stereotype.Component

@Component
class BagSearchOpenbareRuimten : BagSearch() {
    companion object {
        val URI = "direct:bagSearchOpenbareRuimten"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(URI, "openbareruimteIdentificatie", "openbareRuimteIdentificatie")

        searchRoute(
            URI, "zoekOpenbareRuimten", listOf(
                "woonplaatsNaam",
                "openbareRuimteIdentificatie",
                "woonplaatsIdentificatie",
                "huidig",
                "geldigOp",
                "beschikbaarOp",
                "page",
                "pageSize",
                "expand"
            )
        )
    }
}