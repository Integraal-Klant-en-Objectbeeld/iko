package com.ritense.iko.connectors.openzaak.endpoints


class OpenZaakEndpointZaakInformatieObjecten : OpenZaakEndpoint() {
    companion object {
        val URI = "direct:openZaakEndpointZaakInformatieObjecten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(
            URI, "zaakinformatieobject_list", "uuid"
        )

        endpointRoute(
            URI, "zaakinformatieobject_list", listOf(
                "informatieobject",
                "zaak"
            )
        )
    }

}