package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointActorKlantContacten : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointActorKlantContacten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "actorklantcontactenRead", "uuid")

        endpointRoute(
            URI, "actorklantcontactenList", listOf(
                "actor",
                "klantcontact",
                "page",
                "pageSize"
            )
        )
    }
}