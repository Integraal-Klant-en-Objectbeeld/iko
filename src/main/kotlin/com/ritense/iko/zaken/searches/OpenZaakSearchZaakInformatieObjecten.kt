package com.ritense.iko.zaken.searches

import org.springframework.stereotype.Component

@Component
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