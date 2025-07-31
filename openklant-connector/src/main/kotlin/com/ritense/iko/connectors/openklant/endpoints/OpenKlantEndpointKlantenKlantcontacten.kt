package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointKlantenKlantcontacten : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointKlantenKlantcontacten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)
        idRoute(URI, "klant_klantcontact_read")
        endpointRoute(URI, "klant_klantcontact_list")
    }
}
