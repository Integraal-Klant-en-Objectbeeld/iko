package com.ritense.iko.endpoints

import org.springframework.stereotype.Service

@Service
class EndpointService(
    private val endpointRepository: EndpointRepository
) {

    fun getPrimaryEndpoints() = endpointRepository.findAllByIsPrimaryTrueOrderByName()

    fun getEndpoints(): List<Endpoint> = endpointRepository.findAll()
}