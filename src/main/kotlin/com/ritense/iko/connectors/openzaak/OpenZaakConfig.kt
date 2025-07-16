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

@Configuration
@ConditionalOnProperty(name = ["iko.sources.openzaak.enabled"], havingValue = "true", matchIfMissing = true)
class OpenZaakConfig {

    @Bean
    fun openZaak(camelContext: CamelContext, @Value("\${iko.sources.openzaak.host}") host: String) =
        RestOpenApiComponent(camelContext).apply {
            this.specificationUri = "http://localhost:8001/zaken/api/v1/schema/openapi.yaml"
            this.host = host
            this.produces = "application/json"
        }

    @Bean
    fun openZaakApi() = OpenZaakApi()

    @Bean
    fun publicOpenZaakEndpoints() = OpenZaakPublicEndpoints()
    @Bean
    fun openZaakSearchResultaten() = OpenZaakEndpointResultaten()
    @Bean
    fun openZaakSearchRollen() = OpenZaakEndpointRollen()
    @Bean
    fun openZaakSearchStatussen() = OpenZaakEndpointStatussen()
    @Bean
    fun openZaakSearchZaakContactMomenten() = OpenZaakEndpointZaakContactMomenten()
    @Bean
    fun openZaakSearchZaakInformatieObjecten() = OpenZaakEndpointZaakInformatieObjecten()
    @Bean
    fun openZaakSearchZaakObjecten() = OpenZaakEndpointZaakObjecten()
    @Bean
    fun openZaakSearchZaakVerzoeken() = OpenZaakEndpointZaakVerzoeken()
    @Bean
    fun openZaakSearchZaken() = OpenZaakEndpointZaken()
}