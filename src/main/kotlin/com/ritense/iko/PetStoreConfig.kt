package com.ritense.iko

import org.apache.camel.CamelContext
import org.apache.camel.Component
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.rest.openapi.RestOpenApiComponent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class PetStoreConfig {

    @Bean
    fun petstore(camelContext: CamelContext): Component {
        val petstore = RestOpenApiComponent(camelContext)
        petstore.specificationUri = "https://petstore3.swagger.io/api/v3/openapi.json"
        petstore.host = "https://petstore3.swagger.io"
        return petstore
    }

    @Bean
    fun route(): RouteBuilder {
        return PetStoreRoute()
    }
}