package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointKlantenRollen : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointKlantenRollen"
    }

    override fun configure() {
        idAndEndpointRoute(URI)
        idRoute(URI, "klant_rol_read")
        endpointRoute(URI, "klant_rol_list")
    }
}
