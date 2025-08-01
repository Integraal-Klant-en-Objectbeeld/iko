package com.ritense.iko.connectors.bag.endpoints

class BagEndpointOpenbareRuimten : BagEndpoint() {
    companion object {
        val URI = "direct:bagEndpointOpenbareRuimten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "openbareruimteIdentificatie", "openbareRuimteIdentificatie")

        endpointRoute(
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