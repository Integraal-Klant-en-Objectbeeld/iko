package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointPartijIdentificatoren : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointPartijIdentificatoren"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "partijIdentificatorenRead", "uuid")

        endpointRoute(
            URI, "partijIdentificatorenList", listOf(
                "codeObjecttype",
                "codeRegister",
                "codeSoortObjectId",
                "objectId",
                "partij",
                "page",
                "pageSize"
            )
        )
    }
}