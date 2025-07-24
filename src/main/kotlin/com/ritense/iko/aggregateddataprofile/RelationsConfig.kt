package com.ritense.iko.aggregateddataprofile

import com.ritense.iko.endpoints.EndpointRepository
import org.apache.camel.CamelContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RelationsConfig(
    private val camelContext: CamelContext,
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val endpointRepository: EndpointRepository,
) {

    init {
        this.aggregatedDataProfileRepository.findAll().forEach { aggregatedDataProfile ->
            camelContext.addRoutes(
                AggregatedDataProfileRouteBuilder(camelContext, aggregatedDataProfile, endpointRepository)
            )
        }
    }

    @Bean
    fun aggregatedDataProfileRoute() = AggregatedDataProfileRoute(aggregatedDataProfileRepository)

}


