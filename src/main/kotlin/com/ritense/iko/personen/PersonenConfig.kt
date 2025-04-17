package com.ritense.iko.personen

import org.apache.camel.CamelContext
import org.apache.camel.component.rest.openapi.RestOpenApiComponent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PersonenConfig {

    @Bean
    fun brp(camelContext: CamelContext) = RestOpenApiComponent(camelContext).apply {
        this.specificationUri =
            "https://raw.githubusercontent.com/BRP-API/Haal-Centraal-BRP-bevragen/refs/tags/v2.2.1-mock/specificatie/genereervariant/openapi.json"
        this.host = "http://localhost:5010"
        this.produces = "application/json"
    }

    @Bean
    fun brpPersonenSearch() = BrpPersonenSearch()

    @Bean
    fun personenPublicEndpoints() = PersonenPublicEndpoints()

    @Bean
    fun persoonSearch() = PersonenSearch()

    @Bean
    fun personenSearch() = PersonenSearchBsn()

    @Bean
    fun personenSearchPostcodeEndHuisnummer() = PersonenSearchPostcodeEndHuisnummer()

    @Bean
    fun personenValidations() = PersonenValidations()
}