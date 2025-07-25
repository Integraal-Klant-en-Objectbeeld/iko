package com.ritense.iko.endpoints

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class EndpointService(
    private val endpointRepository: EndpointRepository
) {

    fun getEndpoints(): List<Endpoint> = endpointRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
}