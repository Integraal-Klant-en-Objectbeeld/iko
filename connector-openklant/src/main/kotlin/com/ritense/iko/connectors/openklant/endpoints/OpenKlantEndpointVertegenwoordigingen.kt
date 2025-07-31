package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointVertegenwoordigingen : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointVertegenwoordigingen"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "vertegenwoordigingenRead", "uuid")

        endpointRoute(
            URI, "vertegenwoordigingenList", listOf(
                "partij",
                "vertegenwoordiger",
                "page",
                "pageSize"
            )
        )
    }
}