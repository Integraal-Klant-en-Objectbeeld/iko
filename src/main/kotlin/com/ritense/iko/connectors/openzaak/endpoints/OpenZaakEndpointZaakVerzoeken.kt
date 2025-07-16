package com.ritense.iko.connectors.openzaak.endpoints


class OpenZaakEndpointZaakVerzoeken : OpenZaakEndpoint() {
    companion object {
        val URI = "direct:openZaakSearchZaakVerzoeken"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(
            URI, "zaakverzoek_read", "uuid"
        )

        searchRoute(
            URI, "zaakverzoek_list", listOf(
                "verzoek",
                "zaak"
            )
        )
    }

}