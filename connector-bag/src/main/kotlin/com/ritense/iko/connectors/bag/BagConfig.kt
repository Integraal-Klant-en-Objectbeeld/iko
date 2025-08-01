package com.ritense.iko.connectors.bag

import com.ritense.iko.connectors.bag.endpoints.BagEndpointAdresseerbareObjecten
import com.ritense.iko.connectors.bag.endpoints.BagEndpointAdressen
import com.ritense.iko.connectors.bag.endpoints.BagEndpointBronhouders
import com.ritense.iko.connectors.bag.endpoints.BagEndpointLigplaatsen
import com.ritense.iko.connectors.bag.endpoints.BagEndpointNummeraanduidingen
import com.ritense.iko.connectors.bag.endpoints.BagEndpointOpenbareRuimten
import com.ritense.iko.connectors.bag.endpoints.BagEndpointPanden
import com.ritense.iko.connectors.bag.endpoints.BagEndpointStandplaatsen
import com.ritense.iko.connectors.bag.endpoints.BagEndpointVerblijfsobjecten
import com.ritense.iko.connectors.bag.endpoints.BagEndpointWoonplaatsen
import org.apache.camel.CamelContext
import org.apache.camel.component.rest.openapi.RestOpenApiComponent
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
@ConditionalOnProperty(
    value = ["iko.connectors.bag.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class BagConfig {

    @Bean
    fun bag(
        camelContext: CamelContext,
        @Value("\${iko.connectors.bag.host}") host: String,
        @Value("\${iko.connectors.bag.specificationUri}") specificationUri: URI
    ) =
        RestOpenApiComponent(camelContext).apply {
            this.specificationUri = specificationUri.toString()
            this.host = host
            this.produces = "application/json"
        }

    @Bean
    fun bagApi() = BagApi()

    @Bean
    fun bagPublicEndpoints() = BagPublicEndpoints()

    @Bean
    fun bagEndpointAdresseerbareObjecten() = BagEndpointAdresseerbareObjecten()

    @Bean
    fun bagEndpointAdressen() = BagEndpointAdressen()

    @Bean
    fun bagEndpointBronhouders() = BagEndpointBronhouders()

    @Bean
    fun bagEndpointLigplaatsen() = BagEndpointLigplaatsen()

    @Bean
    fun bagEndpointNummeraanduidingen() = BagEndpointNummeraanduidingen()

    @Bean
    fun bagEndpointOpenbareRuimten() = BagEndpointOpenbareRuimten()

    @Bean
    fun bagEndpointPanden() = BagEndpointPanden()

    @Bean
    fun bagEndpointStandplaatsen() = BagEndpointStandplaatsen()

    @Bean
    fun bagEndpointVerblijfsobjecten() = BagEndpointVerblijfsobjecten()

    @Bean
    fun bagEndpointWoonplaatsen() = BagEndpointWoonplaatsen()
}