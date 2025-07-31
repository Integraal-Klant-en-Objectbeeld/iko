package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointContactmomenten : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointContactmomenten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)
        idRoute(URI, "contactmoment_read")
        endpointRoute(URI, "contactmoment_list")
    }
}
