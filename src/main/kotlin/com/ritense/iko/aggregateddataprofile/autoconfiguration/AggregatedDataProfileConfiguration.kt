package com.ritense.iko.aggregateddataprofile.autoconfiguration

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.aggregateddataprofile.camel.AggregatedDataProfileRoute
import com.ritense.iko.aggregateddataprofile.camel.AggregatedDataProfileRouteBuilder
import com.ritense.iko.aggregateddataprofile.error.errorResponse
import com.ritense.iko.aggregateddataprofile.processor.ContainerParamsProcessor
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.cache.processor.CacheProcessor
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import org.apache.camel.CamelContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException

@Configuration
class AggregatedDataProfileConfiguration(
    private val camelContext: CamelContext,
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
    private val cacheProcessor: CacheProcessor,
) {
    init {
        this.aggregatedDataProfileRepository.findAll().forEach { aggregatedDataProfile ->
            val route = AggregatedDataProfileRouteBuilder(
                camelContext,
                aggregatedDataProfile,
                connectorInstanceRepository,
                connectorEndpointRepository,
                cacheProcessor,
            ).apply {
                onException(AccessDeniedException::class.java)
                    .errorResponse(status = HttpStatus.UNAUTHORIZED, exposeMessage = false)

            }
            camelContext.addRoutes(route)
        }
    }

    @Bean
    fun aggregatedDataProfileRoute(
        containerParamsProcessor: ContainerParamsProcessor,
    ) = AggregatedDataProfileRoute(
        aggregatedDataProfileRepository,
        containerParamsProcessor,
    )

    @Bean
    fun containerParamsProcessor(
        objectMapper: ObjectMapper,
    ): ContainerParamsProcessor = ContainerParamsProcessor(objectMapper)
}