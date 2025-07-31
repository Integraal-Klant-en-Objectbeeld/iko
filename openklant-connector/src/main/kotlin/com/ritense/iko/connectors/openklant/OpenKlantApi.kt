package com.ritense.iko.connectors.openklant

import org.apache.camel.builder.RouteBuilder

class OpenKlantApi : RouteBuilder() {
    companion object {
        val URI = "direct:openKlantApi"
    }

    override fun configure() {
        from(URI)
            .errorHandler(noErrorHandler())
            .toD("openKlant:\${header.openKlantApiOperation}")
            .unmarshal().json()
    }
}
