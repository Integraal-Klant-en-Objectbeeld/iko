package com.ritense.iko

import com.ritense.iko.processor.HaalcentraalResponseProcessor
import com.ritense.iko.profile.ProfileRepository
import com.ritense.iko.route.ErrorHandlingRoute
import com.ritense.iko.route.HaalCentraalRoute
import com.ritense.iko.route.MainRoute
import com.ritense.iko.route.ObjectsApiRoute
import com.ritense.iko.route.OpenZaakRoute
import com.ritense.iko.route.PetstoreRoute
import com.ritense.iko.route.RestConfigurationRoute
import com.ritense.iko.route.profiel.persoongegeven.PersoonsgegevensProfielRoute
import com.ritense.iko.route.profiel.persoongegeven.PersoonsgegevensRoute
import com.ritense.iko.route.profiel.zaak.ZakenOpvragenRoute
import com.ritense.iko.route.profiel.zaak.ZakenProfielRoute
import org.apache.camel.CamelContext
import org.apache.camel.component.rest.openapi.RestOpenApiComponent
import org.apache.camel.spi.RestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

@Configuration
class ApplicationConfig(val restConfiguration: RestConfiguration, val profileRepository: ProfileRepository) {

    init {
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun restConfigurationRoute() = RestConfigurationRoute()

    @Bean
    fun petstore(camelContext: CamelContext) = RestOpenApiComponent(camelContext).apply {
        this.specificationUri = "https://petstore3.swagger.io/api/v3/openapi.json"
        this.host = "https://petstore3.swagger.io"
    }

    @Bean
    fun haalcentraal(camelContext: CamelContext) = RestOpenApiComponent(camelContext).apply {
        this.specificationUri =
            "https://raw.githubusercontent.com/BRP-API/Haal-Centraal-BRP-bevragen/refs/tags/v2.2.1-mock/specificatie/genereervariant/openapi.json"
        this.host = "http://localhost:5010"
        this.produces = "application/json"
    }

    @Bean
    fun objectsApi(camelContext: CamelContext) = RestOpenApiComponent(camelContext).apply {
        this.specificationUri = "http://localhost:8010/api/v2/schema/openapi.yaml"
        this.host = "http://localhost:8010"
        this.produces = "application/json"
    }

    @Bean
    fun mainRoute() = MainRoute()

    @Bean
    fun errorHandlingRoute() = ErrorHandlingRoute()

    @Bean
    fun haalcentraalRoute() = HaalCentraalRoute()

    @Bean
    fun objectsApiRoute() = ObjectsApiRoute()

    @Bean
    fun openZaakRoute() = OpenZaakRoute()

    @Bean
    fun petstoreRoute() = PetstoreRoute()

    // Profielen is een main route om een thema
    @Bean
    fun zakenProfielRoute() = ZakenProfielRoute()

    @Bean
    fun zakenOpvragenRoute() = ZakenOpvragenRoute()

    // Persoonsgegevens
    @Bean
    fun persoonsgegevensProfielRoute() = PersoonsgegevensProfielRoute()

    @Bean
    fun persoonsgegevensRoute() = PersoonsgegevensRoute()

    @Bean
    fun haalcentraalResponseProcessor() = HaalcentraalResponseProcessor
}