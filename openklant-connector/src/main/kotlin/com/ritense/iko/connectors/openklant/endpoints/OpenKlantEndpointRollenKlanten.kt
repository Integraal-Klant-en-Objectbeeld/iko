package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointRollenKlanten : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointRollenKlanten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)
        idRoute(URI, "rol_klant_read")
        endpointRoute(URI, "rol_klant_list")
    }
}
