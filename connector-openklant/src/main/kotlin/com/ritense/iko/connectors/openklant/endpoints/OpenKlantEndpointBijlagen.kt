package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointBijlagen : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointBijlagen"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "bijlagenRead", "uuid")

        endpointRoute(
            URI, "bijlagenList", listOf(
                "klantcontact",
                "page",
                "pageSize"
            )
        )
    }
}