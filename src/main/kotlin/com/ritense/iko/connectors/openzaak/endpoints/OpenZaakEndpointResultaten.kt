package com.ritense.iko.connectors.openzaak.endpoints


class OpenZaakEndpointResultaten : OpenZaakEndpoint() {
    companion object {
        val URI = "direct:openZaakSearchResultaten"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(
            URI, "resultaat_read", "uuid"
        )

        searchRoute(
            URI, "resultaat_list", listOf(
                "page",
                "resultaattype",
                "zaak"
            )
        )
    }

}