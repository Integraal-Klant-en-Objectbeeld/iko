package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointRollen : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointRollen"
    }

    override fun configure() {
        idAndEndpointRoute(URI)
        idRoute(URI, "rol_read")
        endpointRoute(URI, "rol_list")
    }
}
