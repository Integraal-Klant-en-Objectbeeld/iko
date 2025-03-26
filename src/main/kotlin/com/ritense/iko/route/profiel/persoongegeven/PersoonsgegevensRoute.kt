package com.ritense.iko.route.profiel.persoongegeven

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.dataformat.JsonLibrary

class PersoonsgegevensRoute : RouteBuilder() {
    override fun configure() {
        from("direct:raadpleegMetBurgerservicenummer")
            .setHeader("Accept", constant("application/json"))
            .setHeader(Exchange.HTTP_URI).header("url")
            .removeHeader(Exchange.HTTP_PATH)
            .removeHeader(Exchange.HTTP_URI)
            .setBody { exchange ->
                val bsn = exchange.getIn().getHeader("bsn", String::class.java)
                mapOf(
                    "type" to "RaadpleegMetBurgerservicenummer",
                    "burgerservicenummer" to listOf(bsn),
                    "fields" to listOf("burgerservicenummer", "naam", "geslacht")
                )
            }
            .marshal().json(JsonLibrary.Jackson)
            .to("haalcentraal:Personen")
    }
}