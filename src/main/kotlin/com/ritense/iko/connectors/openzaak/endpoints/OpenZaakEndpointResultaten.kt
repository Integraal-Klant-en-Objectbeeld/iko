package com.ritense.iko.connectors.openzaak.endpoints


class OpenZaakEndpointResultaten : OpenZaakEndpoint() {
    companion object {
        val URI = "direct:openZaakEndpointResultaten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(
            URI, "resultaat_read", "uuid"
        )

        endpointRoute(
            URI, "resultaat_list", listOf(
                "page",
                "resultaattype",
                "zaak"
            )
        )
    }

}