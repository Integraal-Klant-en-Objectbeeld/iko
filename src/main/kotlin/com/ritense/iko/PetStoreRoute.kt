package com.ritense.iko

import org.apache.camel.builder.RouteBuilder

class PetStoreRoute : RouteBuilder() {
    override fun configure() {
        // The petstore rest endpoint will sometimes return a 404 not found, because it is a dynamic rest endpoint, so you will
        // sometimes get a 500 error on the endpoint and sometimes it will return a json :)
        // Just try it on http://localhost:8080/hello
        from("rest:get:hello")
            .setHeader("petId", constant(1))
            .to("petstore:getPetById")
    }
}