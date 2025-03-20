package com.ritense.iko

import com.ritense.iko.route.ErrorHandlingRoute
import com.ritense.iko.route.HaalcentraalRoute
import com.ritense.iko.route.MainRoute
import com.ritense.iko.route.ObjectsApiRoute
import com.ritense.iko.route.OpenZaakRoute
import com.ritense.iko.route.PetstoreRoute
import org.apache.camel.CamelContext
import org.apache.camel.component.rest.openapi.RestOpenApiComponent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationConfig {

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
    fun openZaak(camelContext: CamelContext) = RestOpenApiComponent(camelContext).apply {
        this.specificationUri = "http://localhost:8001/zaken/api/v1/schema/openapi.yaml"
        this.host = "http://localhost:8001"
        this.produces = "application/json"
    }

    @Bean
    fun mainRoute() = MainRoute()

    @Bean
    fun errorHandlingRoute() = ErrorHandlingRoute()

    @Bean
    fun haalcentraalRoute() = HaalcentraalRoute()

    @Bean
    fun objectsApiRoute() = ObjectsApiRoute()

    @Bean
    fun openZaakRoute() = OpenZaakRoute()

    @Bean
    fun petstoreRoute() = PetstoreRoute()

}