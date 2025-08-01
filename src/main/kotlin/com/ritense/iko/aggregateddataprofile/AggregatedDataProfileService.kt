package com.ritense.iko.aggregateddataprofile

import com.ritense.iko.endpoints.EndpointRepository
import org.apache.camel.CamelContext
import org.springframework.stereotype.Service

@Service
class AggregatedDataProfileService(
    private val camelContext: CamelContext,
    private val endpointRepository: EndpointRepository
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
        camelContext.addRoutes(AggregatedDataProfileRouteBuilder(camelContext, aggregatedDataProfile, endpointRepository))
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