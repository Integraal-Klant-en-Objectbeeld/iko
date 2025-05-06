package com.ritense.iko.source.openzaak.searches


class OpenZaakSearchZaakObjecten : OpenZaakSearch() {
    companion object {
        val URI = "direct:openZaakSearchZaakObjecten"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(
            URI, "zaakobject_read", "uuid"
        )

        searchRoute(
            URI, "zaakobject_list", listOf(
                "object",
                "objectType",
                "page",
                "zaak"
            )
        )
    }

}