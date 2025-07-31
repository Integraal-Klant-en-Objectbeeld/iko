package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointCategorieen : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointCategorieen"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "categorieenRead", "uuid")

        endpointRoute(
            URI, "categorieenList", listOf(
                "naam",
                "page",
                "pageSize"
            )
        )
    }
}