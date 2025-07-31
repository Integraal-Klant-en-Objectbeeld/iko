package com.ritense.iko.connectors.openklant.endpoints

import com.ritense.iko.connectors.openklant.OpenKlantApi
import org.apache.camel.builder.RouteBuilder

class OpenKlantEndpointKlanten : RouteBuilder() {
    companion object {
        val URI = "direct:openKlantEndpointKlanten"
    }

    override fun configure() {
        from(URI)
            .errorHandler(noErrorHandler())
            .choice()
            .`when`(simple("\${header.id} != null"))
            .to("${URI}_id")
            .otherwise()
            .to("${URI}_endpoint")

        from("${URI}_id")
            .errorHandler(noErrorHandler())
            .removeHeaders("*", "id")
            .setHeader("openKlantApiOperation", constant("klant_read"))
            .setHeader("uuid", header("id"))
            .to(OpenKlantApi.URI)

        from("${URI}_endpoint")
            .errorHandler(noErrorHandler())
            .removeHeaders("*", "page", "pageSize")
            .setHeader("openKlantApiOperation", constant("klant_list"))
            .to(OpenKlantApi.URI)
    }
}
