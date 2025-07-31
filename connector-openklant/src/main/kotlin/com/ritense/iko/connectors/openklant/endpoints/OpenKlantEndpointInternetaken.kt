package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointInternetaken : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointInternetaken"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "internetakenRead", "uuid")

        endpointRoute(
            URI, "internetakenList", listOf(
                "klantcontact",
                "page",
                "pageSize"
            )
        )
    }
}