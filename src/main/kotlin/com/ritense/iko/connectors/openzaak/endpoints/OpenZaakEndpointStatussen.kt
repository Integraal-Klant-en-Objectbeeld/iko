package com.ritense.iko.connectors.openzaak.endpoints


class OpenZaakEndpointStatussen : OpenZaakEndpoint() {
    companion object {
        val URI = "direct:openZaakSearchStatussen"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(
            URI, "status_read", "uuid"
        )

        searchRoute(
            URI, "status_list", listOf(
                "indicatieLaatstGezetteStatus",
                "page",
                "statustype",
                "zaak"
            )
        )
    }

}