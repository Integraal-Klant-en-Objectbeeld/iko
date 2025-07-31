package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointDigitaleAdressen : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointDigitaleAdressen"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "digitaleadressenRead", "uuid")

        endpointRoute(
            URI, "digitaleadressenList", listOf(
                "adres",
                "partij",
                "soortDigitaalAdres",
                "page",
                "pageSize"
            )
        )
    }
}