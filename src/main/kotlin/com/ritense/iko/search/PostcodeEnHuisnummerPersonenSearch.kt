package com.ritense.iko.search

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder

class PostcodeEnHuisnummerPersonenSearch : RouteBuilder() {

    companion object {
        val URI = "direct:postcodeEnHuisnummerPersonenSearch"
    }

    override fun configure() {
        from(URI)
            .setHeader("Accept", constant("application/json"))
            .setHeader(Exchange.HTTP_URI).header("url")
            .removeHeader(Exchange.HTTP_PATH)
            .removeHeader(Exchange.HTTP_URI)
            .setBody { exchange ->
                val eIn = exchange.getIn()
                mapOf(
                    "type" to "ZoekMetPostcodeEnHuisnummer",
                    "huisnummer" to eIn.getHeader("huisnummer", Long::class.java),
                    "postcode" to eIn.getHeader("postcode"),
                    "fields" to listOf("burgerservicenummer", "naam", "leeftijd")
                ).filter { v -> v.value != null}
            }
            .marshal().json()
            .to("brp:Personen")
            .transform(jq(".personen"))
    }
}