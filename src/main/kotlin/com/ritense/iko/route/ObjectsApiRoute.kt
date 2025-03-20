package com.ritense.iko.route

import com.ritense.iko.processor.OpenProductResponseProcessor
import org.apache.camel.builder.RouteBuilder

class ObjectsApiRoute : RouteBuilder() {
    override fun configure() {
        from("direct:objectsApi")
            .setHeader("Authorization", constant("Token 182c13e2209161852c53cef53a879f7a2f923430")) // TODO make dynamic
            .setHeader("uuid", constant("1017c4c4-24c1-47b4-8f61-3b45a56f3054")) // TODO refactor to search
            .to("objectsApi:object_read")
            .unmarshal().json()
            .process(OpenProductResponseProcessor)
    }
}