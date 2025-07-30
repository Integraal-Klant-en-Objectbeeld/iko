package com.ritense.iko.connectors.brp

import com.ritense.iko.connectors.brp.endpoints.BrpPersonenEndpoint
import org.apache.camel.CamelContext
import org.apache.camel.component.rest.openapi.RestOpenApiComponent
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
@ConditionalOnProperty(
    value = ["iko.connectors.brp.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class BrpConfig {

    @Bean
    fun brp(
        camelContext: CamelContext,
        @Value("\${iko.connectors.brp.host}") host: String,
        @Value("\${iko.connectors.brp.specificationUri}") specificationUri: URI
    ) =
        RestOpenApiComponent(camelContext).apply {
            this.specificationUri = specificationUri.toString()
            this.host = host
            this.produces = "application/json"
        }

    @Bean
    fun personenValidations() = PersonenValidations()

    @Bean
    fun brpPersonenApi() = BrpPersonenApi()

    @Bean
    fun brpPersonenPublicEndpoints() = BrpPersonenPublicEndpoints()

    @Bean
    fun brpPersonenEndpoint() = BrpPersonenEndpoint()
}