package com.ritense.iko.source.openzaak.searches


class OpenZaakSearchZaakInformatieObjecten : OpenZaakSearch() {
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