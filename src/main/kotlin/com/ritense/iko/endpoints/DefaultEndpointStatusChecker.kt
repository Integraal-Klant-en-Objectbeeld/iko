package com.ritense.iko.endpoints

import org.springframework.stereotype.Component

/**
 * Default implementation of EndpointStatusChecker that uses EndpointService to check if an endpoint is active.
 */
@Component
class DefaultEndpointStatusChecker(
    private val endpointService: EndpointService
) : EndpointStatusChecker {

    /**
     * Check if an endpoint is active by looking it up in the database.
     * @param routeId The route ID of the endpoint to check
     * @return true if the endpoint is active, false otherwise
     */
    override fun isEndpointActive(routeId: String): Boolean {
        return endpointService.isEndpointActive(routeId)
    }
}