package com.ritense.iko.aggregateddataprofile.autoconfiguration

import com.ritense.iko.aggregateddataprofile.camel.AggregatedDataProfileRoute
import com.ritense.iko.aggregateddataprofile.camel.AggregatedDataProfileRouteBuilder
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
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
) {

    init {
        this.aggregatedDataProfileRepository.findAll().forEach { aggregatedDataProfile ->
            camelContext.addRoutes(
                AggregatedDataProfileRouteBuilder(
                    camelContext,
                    aggregatedDataProfile,
                    connectorInstanceRepository,
                    connectorEndpointRepository
                )
            )
        }
    }

    @Bean
    fun aggregatedDataProfileRoute() = AggregatedDataProfileRoute(aggregatedDataProfileRepository)

}