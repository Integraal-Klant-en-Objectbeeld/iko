package com.ritense.iko.endpoints

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class EndpointService(
    private val endpointRepository: EndpointRepository
) {

    fun getEndpoints(): List<Endpoint> = endpointRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))

    fun isEndpointActive(routeId: String): Boolean {
        return try {
            val endpoint = endpointRepository.findByRouteId(routeId)
            endpoint?.isActive ?: false
        } catch (e: Exception) {
            false
        }
    }
}