package com.ritense.iko.source.openzaak.searches


class OpenZaakSearchZaakContactMomenten : OpenZaakSearch() {
    companion object {
        val URI = "direct:openZaakSearchZaakContactMomenten"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(
            URI, "zaakcontactmoment_read", "uuid"
        )

        searchRoute(
            URI, "zaakcontactmoment_list", listOf(
                "contactmoment",
                "zaak"
            )
        )
    }

}