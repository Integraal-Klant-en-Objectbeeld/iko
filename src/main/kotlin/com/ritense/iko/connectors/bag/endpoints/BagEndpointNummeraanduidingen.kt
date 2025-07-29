package com.ritense.iko.connectors.bag.endpoints

class BagEndpointNummeraanduidingen : BagEndpoint() {
    companion object {
        val URI = "direct:bagEndpointNummeraanduidingen"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "nummeraanduidingIdentificatie", "nummeraanduidingIdentificatie")

        endpointRoute(
            URI, "zoekNummeraanduiding", listOf(
                "postcode",
                "huisnummer",
                "huisnummertoevoeging",
                "huisletter",
                "exacteMatch",
                "woonplaatsNaam",
                "openbareRuimteNaam",
                "openbareRuimteIdentificatie",
                "huidig",
                "geldigOp",
                "beschikbaarOp",
                "page",
                "pageSize",
                "expand",
                "pandIdentificatie"
            )
        )

    }
}