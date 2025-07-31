package com.ritense.iko.connectors.openklant

import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointKlanten
import org.apache.camel.CamelContext
import org.apache.camel.component.rest.openapi.RestOpenApiComponent
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
@ConditionalOnProperty(
    value = ["iko.connectors.openklant.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class OpenKlantConfig {

    @Bean
    fun openKlant(
        camelContext: CamelContext,
        @Value("\${iko.connectors.openklant.host}") host: String,
        @Value("\${iko.connectors.openklant.specificationUri}") specificationUri: URI
    ) =
        RestOpenApiComponent(camelContext).apply {
            this.specificationUri = specificationUri.toString()
            this.host = host
            this.produces = "application/json"
        }

    @Bean
    fun openKlantApi() = OpenKlantApi()

    @Bean
    fun openKlantPublicEndpoints() = OpenKlantPublicEndpoints()

    @Bean
    fun openKlantEndpointKlanten() = OpenKlantEndpointKlanten()
}
