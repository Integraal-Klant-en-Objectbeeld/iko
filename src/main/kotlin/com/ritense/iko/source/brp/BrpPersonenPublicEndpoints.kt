package com.ritense.iko.source.brp

import com.ritense.iko.search.PublicSearchEndpoints
import com.ritense.iko.source.brp.search.BrpPersonenSearch
import org.apache.camel.Exchange

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

