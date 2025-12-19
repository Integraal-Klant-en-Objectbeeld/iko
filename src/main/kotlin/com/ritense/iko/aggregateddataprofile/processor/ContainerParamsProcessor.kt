package com.ritense.iko.aggregateddataprofile.processor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.ritense.iko.aggregateddataprofile.camel.ContainerParam
import org.apache.camel.Exchange
import org.springframework.data.domain.Pageable

class ContainerParamsProcessor {

    fun asd(exchange: Exchange) {
        val containerParams: List<ContainerParam> = when (
            val paramHeader = exchange.`in`.getHeader("containerParam")
        ) {
            is List<*> -> paramHeader.map { objectMapper.readValue(it.toString()) }
            is String -> listOf(objectMapper.readValue(paramHeader))
            else -> emptyList()
        }
        val endpointTransformContext: JsonNode = objectMapper
            .valueToTree(
                mapOf(
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

        exchange.`in`.setHeader("iko_endpointTransformContext", endpointTransformContext)
    }
}