package com.ritense.iko.connectors.openklant

import org.apache.camel.builder.RouteBuilder

class OpenKlantApi(val openKlantProperties: OpenKlantProperties) : RouteBuilder() {

    companion object {
        val URI = "direct:openKlantApi"
        const val DEFAULT_INSTANCE = "default"
    }

    override fun configure() {
        from(URI)
            .errorHandler(noErrorHandler())
            .removeHeaders("*")
            .setHeader("Accept", constant("application/json"))
            .process { exchange ->
                val instanceName = exchange.getVariable<String>("config", DEFAULT_INSTANCE, String::class.java)
                val instance = openKlantProperties.instances[instanceName]

                if (instance != null) {
                    // Use the specified instance
                    exchange.getIn().setHeader("Authorization", "Token ${instance.token}")
                    exchange.getIn().setHeader("CamelRestOpenApiComponentName", instanceName)
                } else if (openKlantProperties.token != null) {
                    // Fallback to legacy configuration
                    exchange.getIn().setHeader("Authorization", "Token ${openKlantProperties.token}")
                }
            }
            .toD("openklant.\${variable.config}:\${variable.operation}?throwExceptionOnFailure=false")
            .unmarshal().json()
    }
}