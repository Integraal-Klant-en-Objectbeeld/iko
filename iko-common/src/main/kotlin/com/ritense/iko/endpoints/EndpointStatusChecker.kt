package com.ritense.iko.endpoints

/**
 * Interface for checking if an endpoint is active.
 * This is used by PublicEndpoints to check if an endpoint should be executed.
 */
interface EndpointStatusChecker {
    /**
     * Check if an endpoint is active.
     * @param routeId The route ID of the endpoint to check
     * @return true if the endpoint is active, false otherwise
     */
    fun isEndpointActive(routeId: String): Boolean
}