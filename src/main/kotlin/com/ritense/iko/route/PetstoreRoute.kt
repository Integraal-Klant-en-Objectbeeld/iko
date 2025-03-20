package com.ritense.iko.route

import org.apache.camel.builder.RouteBuilder

class PetstoreRoute : RouteBuilder() {
    override fun configure() {
        from("direct:petstore")
            .setHeader("petId", constant(1))
            .to("petstore:getPetById")
            .unmarshal().json()
    }
}