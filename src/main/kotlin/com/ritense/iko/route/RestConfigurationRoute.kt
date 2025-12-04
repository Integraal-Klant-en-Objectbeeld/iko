package com.ritense.iko.route

import org.apache.camel.builder.RouteBuilder

class RestConfigurationRoute : RouteBuilder() {
    override fun configure() {
        restConfiguration()
            .host("localhost")
            .dataFormatProperty("prettyPrint", "true")
            .apiContextPath("/api-doc")
            .apiProperty("api.title", "IKO Profiel API")
            .apiProperty("api.version", "1.0.0")
            .apiProperty("api.description", "Integraal Klant & Objectbeeld")
            .apiProperty("cors", "true")
    }
}