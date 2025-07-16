package com.ritense.iko.connectors.brp

import com.ritense.iko.connectors.brp.endpoints.BrpPersonenEndpoint
import org.apache.camel.CamelContext
import org.apache.camel.component.rest.openapi.RestOpenApiComponent
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(
    value = ["iko.sources.brp.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class BrpConfig {

    @Bean
    fun brp(camelContext: CamelContext, @Value("\${iko.sources.brp.host}") host: String) =
        RestOpenApiComponent(camelContext).apply {
            this.specificationUri =
                "https://raw.githubusercontent.com/BRP-API/Haal-Centraal-BRP-bevragen/refs/tags/v2.2.1-mock/specificatie/genereervariant/openapi.json"
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
    fun brpPersonenSearch() = BrpPersonenEndpoint()
}