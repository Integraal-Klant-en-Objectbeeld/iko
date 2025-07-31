package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointKlantenContactmomenten : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointKlantenContactmomenten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)
        idRoute(URI, "klant_contactmoment_read")
        endpointRoute(URI, "klant_contactmoment_list")
    }
}
