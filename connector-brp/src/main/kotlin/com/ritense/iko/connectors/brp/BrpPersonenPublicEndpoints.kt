package com.ritense.iko.connectors.brp

import com.ritense.iko.connectors.brp.endpoints.BrpPersonenEndpoint
import com.ritense.iko.endpoints.PublicEndpoints

class BrpPersonenPublicEndpoints : PublicEndpoints() {
    override fun configure() {

        handleAccessDeniedException()

        id("/personen", BrpPersonenEndpoint.URI)
        endpoint("/personen", BrpPersonenEndpoint.URI)
    }

}