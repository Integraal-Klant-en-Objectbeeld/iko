package com.ritense.iko.source.openzaak.searches


class OpenZaakSearchZaakVerzoeken : OpenZaakSearch() {
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