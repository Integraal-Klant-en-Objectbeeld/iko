package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointKlantcontacten : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointKlantcontacten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)
        idRoute(URI, "klantcontact_read")
        endpointRoute(URI, "klantcontact_list")
    }
}
