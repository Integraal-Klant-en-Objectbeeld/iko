package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointKlanten : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointKlanten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)
        idRoute(URI, "klant_read")
        endpointRoute(URI, "klant_list")
    }
}
