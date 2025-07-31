package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointCategorieRelaties : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointCategorieRelaties"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "categorieRelatiesRead", "uuid")

        endpointRoute(
            URI, "categorieRelatiesList", listOf(
                "categorie",
                "klantcontact",
                "page",
                "pageSize"
            )
        )
    }
}