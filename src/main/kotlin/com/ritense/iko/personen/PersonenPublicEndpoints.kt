package com.ritense.iko.personen

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder

class PersonenPublicEndpoints : RouteBuilder() {
    override fun configure() {

        onException(ValidationException::class.java)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setBody(constant("[]"))

        rest("/personen/searchWithBsn")
            .get()
            .to("direct:public_personen_bsn")

        from("direct:public_personen_bsn")
            .to(PersonenSearchBsn.URI)
            .marshal().json()

        rest("/personen/searchWithPostcodeEnHuisnummer")
            .get()
            .to("direct:public_personen_postcodeEnHuisnummer")

        from("direct:public_personen_postcodeEnHuisnummer")
            .to(PersonenSearchPostcodeEndHuisnummer.URI)
            .marshal().json()

        rest("/personen")
            .get("/{id}")
            .to("direct:public_personen")

        from("direct:public_personen")
            .to(PersonenSearch.URI)
            .marshal().json()
    }

}

