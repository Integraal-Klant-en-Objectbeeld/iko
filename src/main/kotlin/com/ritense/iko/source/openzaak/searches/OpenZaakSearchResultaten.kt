package com.ritense.iko.source.openzaak.searches


class OpenZaakSearchResultaten : OpenZaakSearch() {
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