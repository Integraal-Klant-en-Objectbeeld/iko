package com.ritense.iko.connectors.openzaak

import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointResultaten
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointRollen
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointStatussen
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointZaakContactMomenten
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointZaakInformatieObjecten
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointZaakObjecten
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointZaakVerzoeken
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointZaken
import org.apache.camel.CamelContext
import org.apache.camel.component.rest.openapi.RestOpenApiComponent
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
@ConditionalOnProperty(name = ["iko.connectors.openzaak.enabled"], havingValue = "true", matchIfMissing = true)
class OpenZaakConfig {

    @Bean
    fun openZaak(
        camelContext: CamelContext,
        @Value("\${iko.connectors.openzaak.host}") host: String,
        @Value("\${iko.connectors.openzaak.specificationUri}") specificationUri: URI
    ) =
        RestOpenApiComponent(camelContext).apply {
            this.specificationUri = specificationUri.toString()
            this.host = host
            this.produces = "application/json"
        }

    @Bean
    fun openZaakApi() = OpenZaakApi()

    @Bean
    fun publicOpenZaakEndpoints() = OpenZaakPublicEndpoints()

    @Bean
    fun openZaakEndpointResultaten() = OpenZaakEndpointResultaten()

    @Bean
    fun openZaakEndpointRollen() = OpenZaakEndpointRollen()

    @Bean
    fun openZaakEndpointStatussen() = OpenZaakEndpointStatussen()

    @Bean
    fun openZaakEndpointZaakContactMomenten() = OpenZaakEndpointZaakContactMomenten()

    @Bean
    fun openZaakEndpointZaakInformatieObjecten() = OpenZaakEndpointZaakInformatieObjecten()

    @Bean
    fun openZaakEndpointZaakObjecten() = OpenZaakEndpointZaakObjecten()

    @Bean
    fun openZaakEndpointZaakVerzoeken() = OpenZaakEndpointZaakVerzoeken()

    @Bean
    fun openZaakEndpointZaken() = OpenZaakEndpointZaken()
}