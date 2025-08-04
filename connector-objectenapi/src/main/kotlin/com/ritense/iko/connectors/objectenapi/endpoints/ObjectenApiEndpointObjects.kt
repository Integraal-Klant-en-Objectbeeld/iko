package com.ritense.iko.connectors.objectenapi.endpoints

import com.ritense.iko.connectors.objectenapi.ObjectenApiApi
import org.apache.camel.builder.RouteBuilder

class ObjectenApiEndpointObjects : RouteBuilder() {

    companion object {
        val URI = "direct:objectenApiObjecten"
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
            .setHeader("objectenApiOperation", constant("object_read"))
            .setHeader("uuid", header("id"))
            .to(ObjectenApiApi.URI)

        from("${URI}_endpoint")
            .errorHandler(noErrorHandler())
            .removeHeaders(
                "*",
                "data_attrs",
                "data_icontains",
                "date",
                "fields",
                "ordering",
                "page",
                "pageSize",
                "registrationDate",
                "type"
            )
            .setHeader("Accept-Crs", constant("EPSG:4326"))
            .setHeader("objectenApiOperation", constant("object_list"))
            .to(ObjectenApiApi.URI)
    }
}