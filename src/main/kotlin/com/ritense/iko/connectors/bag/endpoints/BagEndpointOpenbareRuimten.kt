package com.ritense.iko.connectors.bag.endpoints

class BagEndpointOpenbareRuimten : BagEndpoint() {
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