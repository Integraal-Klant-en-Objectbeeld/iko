package com.ritense.iko.connectors.openzaak.endpoints


class OpenZaakEndpointZaakContactMomenten : OpenZaakEndpoint() {
    companion object {
        val URI = "direct:openZaakSearchZaakContactMomenten"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(
            URI, "zaakcontactmoment_read", "uuid"
        )

        searchRoute(
            URI, "zaakcontactmoment_list", listOf(
                "contactmoment",
                "zaak"
            )
        )
    }

}