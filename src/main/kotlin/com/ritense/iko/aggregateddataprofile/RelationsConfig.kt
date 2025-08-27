package com.ritense.iko.aggregateddataprofile

import com.ritense.iko.endpoints.EndpointRepository
import com.ritense.iko.poc.db.ConnectorEndpointRepository
import com.ritense.iko.poc.db.ConnectorInstanceRepository
import org.apache.camel.CamelContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RelationsConfig(
    private val camelContext: CamelContext,
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val endpointRepository: EndpointRepository,
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
) {

    init {
        this.aggregatedDataProfileRepository.findAll().forEach { aggregatedDataProfile ->
            camelContext.addRoutes(
                AggregatedDataProfileRouteBuilder(camelContext, aggregatedDataProfile, endpointRepository
                , connectorInstanceRepository, connectorEndpointRepository)
            )
        }
    }

    @Bean
    fun aggregatedDataProfileRoute() = AggregatedDataProfileRoute(aggregatedDataProfileRepository)

}


