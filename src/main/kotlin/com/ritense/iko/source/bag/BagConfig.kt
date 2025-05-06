package com.ritense.iko.source.bag

import com.ritense.iko.source.bag.search.BagSearchAdresseerbareObjecten
import com.ritense.iko.source.bag.search.BagSearchAdressen
import com.ritense.iko.source.bag.search.BagSearchBronhouders
import com.ritense.iko.source.bag.search.BagSearchLigplaatsen
import com.ritense.iko.source.bag.search.BagSearchNummeraanduidingen
import com.ritense.iko.source.bag.search.BagSearchOpenbareRuimten
import com.ritense.iko.source.bag.search.BagSearchPanden
import com.ritense.iko.source.bag.search.BagSearchStandplaatsen
import com.ritense.iko.source.bag.search.BagSearchVerblijfsobjecten
import com.ritense.iko.source.bag.search.BagSearchWoonplaatsen
import org.apache.camel.CamelContext
import org.apache.camel.component.rest.openapi.RestOpenApiComponent
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(
    value = ["iko.sources.bag.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class BagConfig {

    @Bean
    fun bag(camelContext: CamelContext, @Value("\${iko.sources.bag.host}") host: String) =
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
    fun bagSearchAdresseerbareObjecten() = BagSearchAdresseerbareObjecten()

    @Bean
    fun bagSearchAdressen() = BagSearchAdressen()

    @Bean
    fun bagSearchBronhouders() = BagSearchBronhouders()

    @Bean
    fun bagSearchLigplaatsen() = BagSearchLigplaatsen()

    @Bean
    fun bagSearchNummeraanduidingen() = BagSearchNummeraanduidingen()

    @Bean
    fun bagSearchOpenbareRuimten() = BagSearchOpenbareRuimten()

    @Bean
    fun bagSearchPanden() = BagSearchPanden()

    @Bean
    fun bagSearchStandplaatsen() = BagSearchStandplaatsen()

    @Bean
    fun bagSearchVerblijfsobjecten() = BagSearchVerblijfsobjecten()

    @Bean
    fun bagSearchWoonplaatsen() = BagSearchWoonplaatsen()
}