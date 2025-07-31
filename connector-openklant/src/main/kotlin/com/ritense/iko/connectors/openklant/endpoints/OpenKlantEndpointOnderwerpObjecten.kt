package com.ritense.iko.connectors.openklant.endpoints

class OpenKlantEndpointOnderwerpObjecten : OpenKlantEndpoint() {
    companion object {
        val URI = "direct:openKlantEndpointOnderwerpObjecten"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(URI, "onderwerpobjectenRead", "uuid")

        endpointRoute(
            URI, "onderwerpobjectenList", listOf(
                "klantcontact",
                "onderwerpobjectidentificatorCodeObjecttype",
                "onderwerpobjectidentificatorCodeRegister",
                "onderwerpobjectidentificatorCodeSoortObjectId",
                "onderwerpobjectidentificatorObjectId",
                "page",
                "pageSize"
            )
        )
    }
}