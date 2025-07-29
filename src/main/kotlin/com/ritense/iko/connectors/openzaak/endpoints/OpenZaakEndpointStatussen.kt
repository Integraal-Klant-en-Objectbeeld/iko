package com.ritense.iko.connectors.openzaak.endpoints


class OpenZaakEndpointStatussen : OpenZaakEndpoint() {
    companion object {
        val URI = "direct:openZaakEndpointStatussen"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(
            URI, "status_read", "uuid"
        )

        endpointRoute(
            URI, "status_list", listOf(
                "indicatieLaatstGezetteStatus",
                "page",
                "statustype",
                "zaak"
            )
        )
    }

}