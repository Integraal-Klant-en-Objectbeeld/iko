package com.ritense.iko.profile

import com.ritense.iko.endpoints.EndpointRepository
import org.apache.camel.CamelContext
import org.springframework.stereotype.Service

@Service
class ProfileService(
    private val camelContext: CamelContext,
    private val endpointRepository: EndpointRepository
) {

    fun removeRoutes(profile: Profile) {
        removeRoute("profile_${profile.id}_direct")
        removeRoute("profile_${profile.id}_multicast")

        profile.relations.forEach { relation ->
            removeRoute("relation_${relation.id}_direct")
            removeRoute("relation_${relation.id}_multicast")
        }
    }

    fun addRoutes(profile: Profile) {
        camelContext.addRoutes(ProfileRouteBuilder(camelContext, profile, endpointRepository))
    }

    fun reloadRoutes(profile: Profile) {
        removeRoutes(profile)
        addRoutes(profile)
    }

    private fun removeRoute(id: String) {
        camelContext.routeController.stopRoute(id)
        camelContext.removeRoute(id)
    }
}