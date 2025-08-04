package com.ritense.iko.connectors.objectenapi

import com.ritense.iko.connectors.objectenapi.endpoints.ObjectenApiEndpointObjects
import org.apache.camel.CamelContext
import org.apache.camel.component.rest.openapi.RestOpenApiComponent
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
@ConditionalOnProperty(
    value = ["iko.connectors.objectenapi.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class ObjectenApiConfig {
    @Bean
    fun objectenApi(
        camelContext: CamelContext,
        @Value("\${iko.connectors.objectenapi.host}") host: String,
        @Value("\${iko.connectors.objectenapi.specificationUri}") specificationUri: URI
    ) =
        RestOpenApiComponent(camelContext).apply {
            this.specificationUri = specificationUri.toString()
            this.host = host
            this.produces = "application/json"
        }

    @Bean
    fun objectenApiPublicEndpoints() = ObjectenApiPublicEndpoints()

    @Bean
    fun objectenApiApi(@Value("\${iko.connectors.objectenapi.token}") token: String): ObjectenApiApi = ObjectenApiApi(token)

    @Bean
    fun objectenApiEndpointObjects() = ObjectenApiEndpointObjects()
}