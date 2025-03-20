package com.ritense.iko

import com.ritense.iko.openzaak.TokenGeneratorService
import org.apache.camel.AggregationStrategy
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder

class MainRoute : RouteBuilder() {
    override fun configure() {
        // The petstore rest endpoint will sometimes return a 404 not found, because it is a dynamic rest endpoint, so you will
        // sometimes get a 500 error on the endpoint and sometimes it will return a json :)
        // Just try it on http://localhost:8080/hello

        // This code will call 3 different things
        // 1. The haalcentraal docker compose server
        // 2. The petstore json
        // 3. Google.com but this one will fail due to redirection. and will be replaced with "failed!"

        // The outputs of all 3 calls are then aggregated into a single list and returned as result.

        onException(Exception::class.java)
            .to("direct:errorHandle")

        from("rest:get:hello")
            .multicast(ResponseAggregator)
            .parallelProcessing()
            .to("direct:haalcentraal")
            .to("direct:objectsApi")
            .to("direct:openZaak")
            .to("direct:failure")
            //.to("direct:petstore")//
            .end()
            .marshal().json()

        val generatedToken = TokenGeneratorService().generateToken() // TODO refactor using config files
        from("direct:openZaak")
            //.setHeader("Authorization").simple("Bearer $generatedToken")
            .setHeader("Accept-Crs", constant("EPSG:4326"))
            .setHeader("Content-Crs", constant("EPSG:4326"))
            .setHeader("Authorization", constant("Bearer $generatedToken"))
            // zaak id that was created manaully via Openzaak UI
            .setHeader("uuid", constant("68d41e2e-336a-4b4c-82b0-8530bb70bfbc"))  // TODO refactor to search
            .to("openZaak:zaak_read")
            .unmarshal()
            .json()
            .process(ZaakResponseProcessor)

        from("direct:objectsApi")
            .setHeader("Authorization", constant("Token 182c13e2209161852c53cef53a879f7a2f923430")) // TODO make dybamic
            .setHeader("uuid", constant("1017c4c4-24c1-47b4-8f61-3b45a56f3054")) //boom TODO refactor to search
            .to("objectsApi:object_read")
            .unmarshal()
            .json()
            .process(OpenProductResponseProcessor)

        from("direct:failure")
            .routeId("failureRoute")
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            .setHeader(Exchange.HTTP_URI).header("url")
            .removeHeader(Exchange.HTTP_PATH)
            .removeHeader(Exchange.HTTP_URI)
            .to("http://www.google.com")
            .convertBodyTo(String::class.java)

        from("direct:petstore")
            .routeId("petstore")
            .setHeader("petId", constant(1))
            .to("petstore:getPetById")
            .unmarshal()
            .json()

        from("direct:errorHandle")
            //.setBody(constant(mapOf("key" to "Failed")))
            .process { exchange ->
                // Get the failing endpoint (e.g., "direct:petstore", "direct:objectsApi")
                val failedEndpoint = exchange.getProperty(Exchange.FAILURE_ENDPOINT, String::class.java) ?: "unknown"

                // Extract only the direct route name (removes "direct:")
                val failedService = failedEndpoint.substringAfter("direct:")

                // Create an error response with the failing service as the key
                val errorResponse = mapOf(failedService to "Failed")

                // Set response body
                exchange.getIn().body = errorResponse
            }

        from("direct:haalcentraal")
            .setHeader("Accept", constant("application/json"))
            .setHeader(Exchange.HTTP_URI).header("url")
            .removeHeader(Exchange.HTTP_PATH)
            .removeHeader(Exchange.HTTP_URI)
            .setBody { _ ->
                return@setBody """
                    {
                        "type": "RaadpleegMetBurgerservicenummer",
                        "burgerservicenummer": ["999990755"],
                        "fields": [
                            "burgerservicenummer",
                            "naam",
                            "geslacht"
                        ]
                    }
                """.trimIndent()

            }
            .to("haalcentraal:Personen")
            .unmarshal()
            .json()
            .process(HaalcentraalResponseProcessor)
    }
}

object ResponseAggregator : AggregationStrategy {
    override fun aggregate(oldExchange: Exchange?, newExchange: Exchange): Exchange {
        if (oldExchange == null) {
            return newExchange
        }
        val oldBody = oldExchange.getIn().body as Map<*, *>
        val newBody = newExchange.getIn().body as Map<*, *>
        oldExchange.getIn().body = oldBody + newBody
        return oldExchange
    }
}

object OpenProductResponseProcessor : Processor {
    override fun process(exchange: Exchange) {
        val body = exchange.getIn().body
        val processedBody = mapOf("product" to body)
        exchange.getIn().body = processedBody
    }
}

object HaalcentraalResponseProcessor : Processor {
    override fun process(exchange: Exchange) {
        val body = exchange.getIn().body
        val processedBody = mapOf("brp" to body)
        exchange.getIn().body = processedBody
    }
}

object ZaakResponseProcessor : Processor {
    override fun process(exchange: Exchange) {
        val body = exchange.getIn().body
        val processedBody = mapOf("zaak" to body)
        exchange.getIn().body = processedBody
    }
}


