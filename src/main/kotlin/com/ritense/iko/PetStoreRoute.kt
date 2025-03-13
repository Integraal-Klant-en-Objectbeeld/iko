package com.ritense.iko

import org.apache.camel.builder.RouteBuilder

class PetStoreRoute : RouteBuilder() {
    override fun configure() {
        from("rest:get:hello")
            .setHeader("petId", constant(1))
            .to("petstore:getPetById")
    }
}