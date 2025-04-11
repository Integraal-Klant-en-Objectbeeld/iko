package com.ritense.iko.source

import org.apache.camel.CamelContext
import org.apache.camel.component.rest.openapi.RestOpenApiComponent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ZakenApiConfig {

    @Bean
    fun zakenApi(camelContext: CamelContext) = RestOpenApiComponent(camelContext).apply {
        this.specificationUri = "http://localhost:8001/zaken/api/v1/schema/openapi.yaml"
        this.host = "http://localhost:8001"
        this.produces = "application/json"
    }

}