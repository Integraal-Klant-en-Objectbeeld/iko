package com.ritense.iko.source

import org.apache.camel.CamelContext
import org.apache.camel.component.rest.openapi.RestOpenApiComponent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ObjectenApiConfig {

    @Bean
    fun objectenApi(camelContext: CamelContext) = RestOpenApiComponent(camelContext).apply {
        this.specificationUri = "http://localhost:8010/api/v2/schema/openapi.yaml"
        this.host = "http://localhost:8010"
        this.produces = "application/json"
    }

}