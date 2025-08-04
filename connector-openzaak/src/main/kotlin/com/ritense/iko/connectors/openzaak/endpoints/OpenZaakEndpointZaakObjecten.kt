package com.ritense.iko.connectors.openzaak.endpoints


class OpenZaakEndpointZaakObjecten : OpenZaakEndpoint() {
    companion object {
        val URI = "direct:openZaakEndpointZaakObjecten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(
            URI, "zaakobject_read", "uuid"
        )

        endpointRoute(
            URI, "zaakobject_list", listOf(
                "object",
                "objectType",
                "page",
                "zaak"
            )
        )
    }

}