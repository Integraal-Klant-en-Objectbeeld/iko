package com.ritense.iko.search

import org.apache.camel.builder.RouteBuilder

class PublicPersonenSearch : RouteBuilder() {
    override fun configure() {
        rest("/personen/searchWithBsn")
            .get()
            .to("direct:personenSearch")

        rest("/personen/searchWithPostcodeEnHuisnummer")
            .get()
            .to("direct:postcodeEnHuisnummerPersonenSearch")

        rest("/personen")
            .get("/{bsn}")
            .to("direct:persoonSearch")
    }

}
