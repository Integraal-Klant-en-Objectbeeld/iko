package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointContactmomentenKlantcontacten : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointContactmomentenKlantcontacten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)
        idRoute(URI, "contactmoment_klantcontact_read")
        endpointRoute(URI, "contactmoment_klantcontact_list")
    }
}
