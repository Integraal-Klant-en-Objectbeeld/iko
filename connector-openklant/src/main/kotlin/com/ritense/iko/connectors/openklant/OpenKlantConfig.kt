package com.ritense.iko.connectors.openklant

import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointActorKlantContacten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointActoren
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointBetrokkenen
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointBijlagen
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointCategorieRelaties
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointCategorieen
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointDigitaleAdressen
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointInternetaken
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointKlantContacten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointOnderwerpObjecten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointPartijIdentificatoren
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointPartijen
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointRekeningnummers
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointVertegenwoordigingen
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
    matchIfMissing = false
)
class OpenKlantConfig {

    @Bean
    fun openklant(
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

    // Beans for each endpoint
    @Bean
    fun openKlantEndpointActoren() = OpenKlantEndpointActoren()

    @Bean
    fun openKlantEndpointActorKlantContacten() = OpenKlantEndpointActorKlantContacten()

    @Bean
    fun openKlantEndpointBetrokkenen() = OpenKlantEndpointBetrokkenen()

    @Bean
    fun openKlantEndpointBijlagen() = OpenKlantEndpointBijlagen()

    @Bean
    fun openKlantEndpointCategorieRelaties() = OpenKlantEndpointCategorieRelaties()

    @Bean
    fun openKlantEndpointCategorieen() = OpenKlantEndpointCategorieen()

    @Bean
    fun openKlantEndpointDigitaleAdressen() = OpenKlantEndpointDigitaleAdressen()

    @Bean
    fun openKlantEndpointInternetaken() = OpenKlantEndpointInternetaken()

    @Bean
    fun openKlantEndpointKlantContacten() = OpenKlantEndpointKlantContacten()

    @Bean
    fun openKlantEndpointOnderwerpObjecten() = OpenKlantEndpointOnderwerpObjecten()

    @Bean
    fun openKlantEndpointPartijIdentificatoren() = OpenKlantEndpointPartijIdentificatoren()

    @Bean
    fun openKlantEndpointPartijen() = OpenKlantEndpointPartijen()

    @Bean
    fun openKlantEndpointRekeningnummers() = OpenKlantEndpointRekeningnummers()

    @Bean
    fun openKlantEndpointVertegenwoordigingen() = OpenKlantEndpointVertegenwoordigingen()
}