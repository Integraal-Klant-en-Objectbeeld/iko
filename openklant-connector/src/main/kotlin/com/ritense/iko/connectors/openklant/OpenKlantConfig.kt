package com.ritense.iko.connectors.openklant

import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointKlanten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointKlantcontacten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointContactmomenten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointRollen
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointKlantenContactmomenten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointKlantenKlantcontacten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointKlantenRollen
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointKlantcontactenContactmomenten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointContactmomentenKlantcontacten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointRollenKlanten
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

    @Bean
    fun openKlantEndpointKlantcontacten() = OpenKlantEndpointKlantcontacten()

    @Bean
    fun openKlantEndpointContactmomenten() = OpenKlantEndpointContactmomenten()

    @Bean
    fun openKlantEndpointRollen() = OpenKlantEndpointRollen()

    @Bean
    fun openKlantEndpointKlantenContactmomenten() = OpenKlantEndpointKlantenContactmomenten()

    @Bean
    fun openKlantEndpointKlantenKlantcontacten() = OpenKlantEndpointKlantenKlantcontacten()

    @Bean
    fun openKlantEndpointKlantenRollen() = OpenKlantEndpointKlantenRollen()

    @Bean
    fun openKlantEndpointKlantcontactenContactmomenten() = OpenKlantEndpointKlantcontactenContactmomenten()

    @Bean
    fun openKlantEndpointContactmomentenKlantcontacten() = OpenKlantEndpointContactmomentenKlantcontacten()

    @Bean
    fun openKlantEndpointRollenKlanten() = OpenKlantEndpointRollenKlanten()
}
