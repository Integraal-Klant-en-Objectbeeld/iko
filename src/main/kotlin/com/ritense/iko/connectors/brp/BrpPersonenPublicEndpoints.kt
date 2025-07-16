package com.ritense.iko.connectors.brp

import com.ritense.iko.endpoints.PublicEndpoints
import com.ritense.iko.connectors.brp.endpoints.BrpPersonenEndpoint
import org.apache.camel.Exchange

class BrpPersonenPublicEndpoints : PublicEndpoints() {
    override fun configure() {

        onException(ValidationException::class.java)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setBody(constant("[]"))

        id("/personen", BrpPersonenEndpoint.URI)
        search("/personen", BrpPersonenEndpoint.URI)
    }

}

