package com.ritense.iko.aggregateddataprofile.autoconfiguration

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.aggregateddataprofile.camel.AggregatedDataProfileRoute
import com.ritense.iko.aggregateddataprofile.camel.AggregatedDataProfileRouteBuilder
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.cache.processor.CacheProcessor
import com.ritense.iko.cache.service.CacheService
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import org.apache.camel.CamelContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AggregatedDataProfileConfiguration(
    private val camelContext: CamelContext,
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
    private val cacheService: CacheService,
    private val objectMapper: ObjectMapper,
    private val ikoCacheProcessor: CacheProcessor
) {

    init {
        this.aggregatedDataProfileRepository.findAll().forEach { aggregatedDataProfile ->
            camelContext.addRoutes(
                AggregatedDataProfileRouteBuilder(
                    camelContext,
                    aggregatedDataProfile,
                    connectorInstanceRepository,
                    connectorEndpointRepository,
                    ikoCacheProcessor
                )
            )
        }
    }

    @Bean
    fun aggregatedDataProfileRoute() = AggregatedDataProfileRoute(
        aggregatedDataProfileRepository,
        cacheService,
        objectMapper
    )

}