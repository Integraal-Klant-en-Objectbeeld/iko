package com.ritense.iko.source.openzaak

import com.ritense.iko.source.openzaak.searches.OpenZaakSearchResultaten
import com.ritense.iko.source.openzaak.searches.OpenZaakSearchRollen
import com.ritense.iko.source.openzaak.searches.OpenZaakSearchStatussen
import com.ritense.iko.source.openzaak.searches.OpenZaakSearchZaakContactMomenten
import com.ritense.iko.source.openzaak.searches.OpenZaakSearchZaakInformatieObjecten
import com.ritense.iko.source.openzaak.searches.OpenZaakSearchZaakObjecten
import com.ritense.iko.source.openzaak.searches.OpenZaakSearchZaakVerzoeken
import com.ritense.iko.source.openzaak.searches.OpenZaakSearchZaken
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
    fun publicOpenZaakEndpoints() = PublicOpenZaakEndpoints()
    @Bean
    fun openZaakSearchResultaten() = OpenZaakSearchResultaten()
    @Bean
    fun openZaakSearchRollen() = OpenZaakSearchRollen()
    @Bean
    fun openZaakSearchStatussen() = OpenZaakSearchStatussen()
    @Bean
    fun openZaakSearchZaakContactMomenten() = OpenZaakSearchZaakContactMomenten()
    @Bean
    fun openZaakSearchZaakInformatieObjecten() = OpenZaakSearchZaakInformatieObjecten()
    @Bean
    fun openZaakSearchZaakObjecten() = OpenZaakSearchZaakObjecten()
    @Bean
    fun openZaakSearchZaakVerzoeken() = OpenZaakSearchZaakVerzoeken()
    @Bean
    fun openZaakSearchZaken() = OpenZaakSearchZaken()
}