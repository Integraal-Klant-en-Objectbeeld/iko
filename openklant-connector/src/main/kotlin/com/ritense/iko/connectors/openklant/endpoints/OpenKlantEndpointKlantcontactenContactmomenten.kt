package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointKlantcontactenContactmomenten : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointKlantcontactenContactmomenten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)
        idRoute(URI, "klantcontact_contactmoment_read")
        endpointRoute(URI, "klantcontact_contactmoment_list")
    }
}
