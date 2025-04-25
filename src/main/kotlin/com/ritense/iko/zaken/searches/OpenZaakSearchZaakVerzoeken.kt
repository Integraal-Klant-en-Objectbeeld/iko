package com.ritense.iko.zaken.searches

import org.springframework.stereotype.Component

@Component
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