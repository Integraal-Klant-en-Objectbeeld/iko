package com.ritense.iko.aggregateddataprofile.service

import com.ritense.iko.aggregateddataprofile.camel.AggregatedDataProfileRouteBuilder
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.cache.CacheService
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import org.apache.camel.CamelContext
import org.springframework.stereotype.Service

@Service
class AggregatedDataProfileService(
    private val camelContext: CamelContext,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val cacheService: CacheService
) {

    fun removeRoutes(aggregatedDataProfile: AggregatedDataProfile) {
        removeRoute("aggregated_data_profile_${aggregatedDataProfile.id}_direct")
        removeRoute("aggregated_data_profile_${aggregatedDataProfile.id}_multicast")

        aggregatedDataProfile.relations.forEach { relation ->
            removeRoute("relation_${relation.id}_direct")
            removeRoute("relation_${relation.id}_multicast")
        }
    }

    fun addRoutes(aggregatedDataProfile: AggregatedDataProfile) {
        camelContext.addRoutes(
            AggregatedDataProfileRouteBuilder(
                camelContext,
                aggregatedDataProfile,
                connectorEndpointRepository = connectorEndpointRepository,
                connectorInstanceRepository = connectorInstanceRepository,
                cacheService = cacheService
            )
        )
    }

    fun reloadRoutes(aggregatedDataProfile: AggregatedDataProfile) {
        removeRoutes(aggregatedDataProfile)
        addRoutes(aggregatedDataProfile)
    }

    private fun removeRoute(id: String) {
        camelContext.routeController.stopRoute(id)
        camelContext.removeRoute(id)
    }
}