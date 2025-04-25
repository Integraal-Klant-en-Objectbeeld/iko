package com.ritense.iko.bag

import org.apache.camel.CamelContext
import org.apache.camel.component.rest.openapi.RestOpenApiComponent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BagConfig {

    @Bean
    fun bag(camelContext: CamelContext) = RestOpenApiComponent(camelContext).apply {
        this.specificationUri =
            "https://api.bag.kadaster.nl/lvbag/individuelebevragingen/v2/openapi.yaml"
        this.host = "https://api.bag.kadaster.nl"
        this.produces = "application/json"
        this.isRequestValidationEnabled = true
    }
}