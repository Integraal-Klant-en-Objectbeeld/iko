package com.ritense.iko.route

import com.ritense.iko.processor.HaalcentraalResponseProcessor
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder

class HaalCentraalRoute : RouteBuilder() {
    override fun configure() {
        from("direct:haalcentraal")
            .setHeader("Accept", constant("application/json"))
            .setHeader(Exchange.HTTP_URI).header("url")
            .removeHeader(Exchange.HTTP_PATH)
            .removeHeader(Exchange.HTTP_URI)
            .setBody {
                """
                {
                    "type": "RaadpleegMetBurgerservicenummer",
                    "burgerservicenummer": ["999990755"],
                    "fields": ["burgerservicenummer", "naam", "geslacht"]
                }
                """.trimIndent()
            }
            .to("haalcentraal:Personen")
            .unmarshal().json()
            .process(HaalcentraalResponseProcessor)
    }
}