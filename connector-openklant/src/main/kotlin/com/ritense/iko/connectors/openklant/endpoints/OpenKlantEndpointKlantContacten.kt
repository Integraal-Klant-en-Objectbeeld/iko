package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointKlantContacten : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointKlantContacten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "klantcontactenRead", "uuid")

        endpointRoute(
            URI, "klantcontactenList", listOf(
                "onderwerp",
                "plaatsgevondenVanaf",
                "plaatsgevondenTot",
                "kanaal",
                "status",
                "indicatieContactGelukt",
                "page",
                "pageSize"
            )
        )
    }
}