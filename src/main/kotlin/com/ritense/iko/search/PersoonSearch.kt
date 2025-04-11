package com.ritense.iko.search

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder

class PersoonSearch : RouteBuilder() {

    companion object {
        val URI = "direct:persoonSearch"
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
                    "burgerservicenummer" to listOf(exchange.getIn().getHeader("bsn")),
                    "fields" to listOf("burgerservicenummer", "naam", "geslacht", "kinderen")
                )
            }
            .marshal().json()
            .to("brp:Personen")
            .transform(jq(".personen[0]"))
    }
}