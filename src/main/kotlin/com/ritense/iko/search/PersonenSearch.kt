package com.ritense.iko.search

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.dataformat.JsonLibrary

class PersonenSearch : RouteBuilder() {

    companion object {
        val URI = "direct:personenSearch"
    }

    override fun configure() {
        from(URI)
            .setHeader("Accept", constant("application/json"))
            .setHeader(Exchange.HTTP_URI).header("url")
            .removeHeader(Exchange.HTTP_PATH)
            .removeHeader(Exchange.HTTP_URI)
            .setBody { exchange ->
                mapOf(
                    "type" to "RaadpleegMetBurgerservicenummer",
                    "burgerservicenummer" to listOf(exchange.getIn().getHeader("bsn", String::class.java)),
                    "fields" to listOf("burgerservicenummer", "naam")
                )
            }
            .marshal().json(JsonLibrary.Jackson)
            .to("brp:Personen")
            .transform(jq(".personen"))
    }
}