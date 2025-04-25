package com.ritense.iko.brp

import com.ritense.iko.brp.search.BrpPersonenSearch
import com.ritense.iko.search.PublicSearchEndpoints
import org.apache.camel.Exchange
import org.springframework.stereotype.Component

@Component
class BrpPersonenPublicEndpoints : PublicSearchEndpoints() {
    override fun configure() {

        onException(ValidationException::class.java)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setBody(constant("[]"))

        id("/personen", BrpPersonenSearch.URI)
        search("/personen", BrpPersonenSearch.URI)
    }

}

