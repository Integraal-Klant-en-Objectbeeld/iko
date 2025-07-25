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

@Configuration
@ConditionalOnProperty(
    value = ["iko.connectors.bag.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class BagConfig {

    @Bean
    fun bag(camelContext: CamelContext, @Value("\${iko.connectors.bag.host}") host: String) =
        RestOpenApiComponent(camelContext).apply {
            this.specificationUri =
                "https://api.bag.kadaster.nl/lvbag/individuelebevragingen/v2/openapi.yaml"
            this.host = host
            this.produces = "application/json"
        }

    @Bean
    fun bagApi() = BagApi()

    @Bean
    fun bagPublicEndpoints() = BagPublicEndpoints()

    @Bean
    fun bagSearchAdresseerbareObjecten() = BagEndpointAdresseerbareObjecten()

    @Bean
    fun bagSearchAdressen() = BagEndpointAdressen()

    @Bean
    fun bagSearchBronhouders() = BagEndpointBronhouders()

    @Bean
    fun bagSearchLigplaatsen() = BagEndpointLigplaatsen()

    @Bean
    fun bagSearchNummeraanduidingen() = BagEndpointNummeraanduidingen()

    @Bean
    fun bagSearchOpenbareRuimten() = BagEndpointOpenbareRuimten()

    @Bean
    fun bagSearchPanden() = BagEndpointPanden()

    @Bean
    fun bagSearchStandplaatsen() = BagEndpointStandplaatsen()

    @Bean
    fun bagSearchVerblijfsobjecten() = BagEndpointVerblijfsobjecten()

    @Bean
    fun bagSearchWoonplaatsen() = BagEndpointWoonplaatsen()
}