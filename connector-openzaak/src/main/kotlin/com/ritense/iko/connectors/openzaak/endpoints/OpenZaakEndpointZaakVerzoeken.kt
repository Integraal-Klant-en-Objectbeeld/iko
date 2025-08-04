package com.ritense.iko.connectors.openzaak.endpoints


class OpenZaakEndpointZaakVerzoeken : OpenZaakEndpoint() {
    companion object {
        val URI = "direct:openZaakEndpointZaakVerzoeken"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(
            URI, "zaakverzoek_read", "uuid"
        )

        endpointRoute(
            URI, "zaakverzoek_list", listOf(
                "verzoek",
                "zaak"
            )
        )
    }

}