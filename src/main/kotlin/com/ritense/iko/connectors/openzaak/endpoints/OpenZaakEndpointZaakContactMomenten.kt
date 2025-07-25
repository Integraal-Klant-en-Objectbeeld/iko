package com.ritense.iko.connectors.openzaak.endpoints


class OpenZaakEndpointZaakContactMomenten : OpenZaakEndpoint() {
    companion object {
        val URI = "direct:openZaakEndpointZaakContactMomenten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(
            URI, "zaakcontactmoment_read", "uuid"
        )

        endpointRoute(
            URI, "zaakcontactmoment_list", listOf(
                "contactmoment",
                "zaak"
            )
        )
    }

}