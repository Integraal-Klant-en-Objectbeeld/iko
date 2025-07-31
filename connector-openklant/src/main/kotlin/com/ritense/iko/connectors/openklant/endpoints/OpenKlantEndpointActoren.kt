package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointActoren : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointActoren"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "actorenRead", "uuid")

        endpointRoute(
            URI, "actorenList", listOf(
                "actoridentificatorCodeObjecttype",
                "actoridentificatorCodeRegister",
                "actoridentificatorCodeSoortObjectId",
                "actoridentificatorObjectId",
                "actortype",
                "indicatieActief",
                "indicatieGeheimhouding",
                "naam",
                "page",
                "pageSize"
            )
        )
    }
}