package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointBetrokkenen : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointBetrokkenen"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "betrokkenenRead", "uuid")

        endpointRoute(
            URI, "betrokkenenList", listOf(
                "klantcontact",
                "rol",
                "page",
                "pageSize"
            )
        )
    }
}