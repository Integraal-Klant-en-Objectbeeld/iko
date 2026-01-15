package com.ritense.iko.aggregateddataprofile.processor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ritense.iko.aggregateddataprofile.camel.ContainerParam
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.ADP_CONTAINER_PARAM_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.ADP_ENDPOINT_TRANSFORM_CONTEXT_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.ADP_ID_PARAM_HEADER
import com.ritense.iko.aggregateddataprofile.error.AggregatedDataProfileQueryParametersError
import org.apache.camel.Exchange
import org.springframework.data.domain.Pageable
import kotlin.io.encoding.Base64

class ContainerParamsProcessor(
    private val objectMapper: ObjectMapper,
) {

    fun process(exchange: Exchange) {
        with(exchange.`in`) {
            val idParam: String? = getHeader(ADP_ID_PARAM_HEADER, String::class.java)
            val decodedContainerParamsHeader: List<String> =
                decodeContainerParams(
                    header = getHeader(ADP_CONTAINER_PARAM_HEADER),
                )
            val containerParams =
                runCatching<List<ContainerParam>> {
                    decodedContainerParamsHeader.map { objectMapper.readValue(it) }
                }.getOrNull()
                    ?: throw AggregatedDataProfileQueryParametersError("containerParam")
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

    private fun decodeContainerParams(header: Any): List<String> = when (header) {
        is List<*> -> {
            header
                .map {
                    runCatching {
                        it as String
                        String(Base64.decode(it), Charsets.UTF_8)
                    }.getOrNull()
                        ?: throw AggregatedDataProfileQueryParametersError("containerParam")
                }
        }

        is String -> {
            runCatching {
                listOf(
                    String(Base64.decode(header), Charsets.UTF_8),
                )
            }.getOrNull()
                ?: throw AggregatedDataProfileQueryParametersError("containerParam")
        }

        else -> emptyList()
    }
}