package com.ritense.iko.connectors.objectenapi

import org.apache.camel.builder.RouteBuilder

class ObjectenApiApi(val token: String) : RouteBuilder() {

    companion object {
        val URI = "direct:objectenApi"
    }

    override fun configure() {
        from(URI)
            .errorHandler(noErrorHandler())
            .setHeader("Authorization", { "Token ${token}" })
            .toD("objectenApi:\${header.objectenApiOperation}")
            .unmarshal().json()
    }
}