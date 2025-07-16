package com.ritense.iko.connectors.openzaak.endpoints


class OpenZaakEndpointZaakInformatieObjecten : OpenZaakEndpoint() {
    companion object {
        val URI = "direct:openZaakSearchZaakInformatieObjecten"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(
            URI, "zaakinformatieobject_list", "uuid"
        )

        searchRoute(
            URI, "zaakinformatieobject_list", listOf(
                "informatieobject",
                "zaak"
            )
        )
    }

}