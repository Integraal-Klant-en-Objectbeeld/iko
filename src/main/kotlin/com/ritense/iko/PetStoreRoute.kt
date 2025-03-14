package com.ritense.iko

import org.apache.camel.AggregationStrategy
import org.apache.camel.Exchange
import org.apache.camel.Expression
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.http.base.HttpOperationFailedException

class PetStoreRoute : RouteBuilder() {
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
            .multicast(AggregationResults())
            .parallelProcessing()
            .to("direct:haalcentraal")
            .to("direct:petstore")
            .to("direct:failure")
            .end().marshal().json()

        from("direct:failure")
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            .setHeader(Exchange.HTTP_URI).header("url")
            .removeHeader(Exchange.HTTP_PATH)
            .removeHeader(Exchange.HTTP_URI)
            .to("http://www.google.com")
            .convertBodyTo(String::class.java)

        from("direct:petstore")
            .setHeader("petId", constant(1))
            .to("petstore:getPetById").unmarshal().json()

        from("direct:errorHandle")
            .setBody(constant("failed!"))


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
            .to("haalcentraal:Personen").unmarshal().json()
    }
}

class AggregationResults : AggregationStrategy {
    override fun aggregate(oldExchange: Exchange?, newExchange: Exchange?): Exchange {
        if (oldExchange == null) {
            return newExchange!!
        }

        oldExchange.`in`.body = listOf(oldExchange!!.`in`!!.getBody(), newExchange!!.`in`!!.getBody())

        return oldExchange;
    }

}