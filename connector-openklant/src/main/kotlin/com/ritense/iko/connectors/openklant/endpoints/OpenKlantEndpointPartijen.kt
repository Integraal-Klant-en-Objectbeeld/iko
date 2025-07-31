package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointPartijen : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointPartijen"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "partijenRead", "uuid")

        endpointRoute(
            URI, "partijenList", listOf(
                "naam",
                "partijidentificatorCodeObjecttype",
                "partijidentificatorCodeRegister",
                "partijidentificatorCodeSoortObjectId",
                "partijidentificatorObjectId",
                "partijsoort",
                "page",
                "pageSize"
            )
        )
    }
}