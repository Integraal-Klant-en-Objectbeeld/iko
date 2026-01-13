package com.ritense.iko.aggregateddataprofile.processor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ritense.iko.aggregateddataprofile.camel.ContainerParam
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.ADP_CONTAINER_PARAM_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.ADP_ENDPOINT_TRANSFORM_CONTEXT_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.ADP_ID_PARAM_HEADER
import org.apache.camel.Exchange
import org.springframework.data.domain.Pageable

class ContainerParamsProcessor(
    private val objectMapper: ObjectMapper,
) {

    fun process(exchange: Exchange) {
        with(exchange.`in`) {
            val idParam: String? = getHeader(ADP_ID_PARAM_HEADER, String::class.java)
            val containerParams: List<ContainerParam> = when (
                val paramHeader = getHeader(ADP_CONTAINER_PARAM_HEADER)
            ) {
                is List<*> -> paramHeader.map { objectMapper.readValue(it.toString()) }
                is String -> listOf(objectMapper.readValue(paramHeader))
                else -> emptyList()
            }
            val endpointTransformContext: JsonNode = objectMapper
                .valueToTree(
                    mapOf(
                        "idParam" to idParam,
                        "sortParams" to
                            containerParams
                                .filter { !Pageable.unpaged().equals(it.pageable) }
                                .associate { it.containerId to it.pageable },
                        "filterParams" to
                            containerParams
                                .filter { it.filters.isNotEmpty() }
                                .associate { it.containerId to it.filters },
                    ),
                )

            setHeader(ADP_ENDPOINT_TRANSFORM_CONTEXT_HEADER, endpointTransformContext)
            removeHeader(ADP_ID_PARAM_HEADER)
            removeHeader(ADP_CONTAINER_PARAM_HEADER)
        }
    }
}