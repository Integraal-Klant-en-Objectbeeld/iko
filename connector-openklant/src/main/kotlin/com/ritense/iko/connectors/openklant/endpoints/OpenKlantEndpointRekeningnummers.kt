package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointRekeningnummers : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointRekeningnummers"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "rekeningnummersRead", "uuid")

        endpointRoute(
            URI, "rekeningnummersList", listOf(
                "iban",
                "partij",
                "page",
                "pageSize"
            )
        )
    }
}