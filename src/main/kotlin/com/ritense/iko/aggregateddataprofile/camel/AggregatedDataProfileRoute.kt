package com.ritense.iko.aggregateddataprofile.camel

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.ParamDefinition
import org.apache.camel.model.rest.RestParamType.query
import org.springframework.data.domain.Pageable

class AggregatedDataProfileRoute(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val objectMapper: ObjectMapper,
) : RouteBuilder() {
    override fun configure() {
        val containerParamsParamDefinition = ParamDefinition()
            .name("containerParam")
            .description("Container parameters for ADP")
            .type(query)
            .arrayType("String")
            .required(false)

        rest("/aggregated-data-profiles")
            .description("Resolve ADP by profile name")
            .get("/{iko_profile}")
            .param(containerParamsParamDefinition)
            .to("direct:rest-to-adp")
            .get("/{iko_profile}/{iko_id}")
            .param(containerParamsParamDefinition)
            .to("direct:rest-to-adp-with-id")

        from("direct:rest-to-adp")
            .to("direct:aggregated-data-profile-container-params")

        from("direct:rest-to-adp-with-id")
            .to("direct:aggregated-data-profile-container-params")

        from("direct:aggregated-data-profile-container-params")
            .routeId("aggregated-data-profile-container-params")
            .process { exchange ->
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
                            "adpSortParams" to
                                containerParams
                                    .filter { !Pageable.unpaged().equals(it.pageable) }
                                    .associate { it.containerId to it.pageable },
                            "adpFilterParams" to
                                containerParams
                                    .filter { it.filters.isNotEmpty() }
                                    .associate { it.containerId to it.filters },
                        ),
                    )

                exchange.`in`.setHeader("iko_endpointTransformContext", endpointTransformContext)
            }
            .removeHeader("containerParam")
            .to("direct:aggregated_data_profile_rest_continuation")

        from("direct:aggregated_data_profile_rest_continuation")
            .routeId("aggregated-data-profile-rest-continuation")
            .setVariable("id", header("iko_id"))
            .setVariable("profile", header("iko_profile"))
            .setVariable(ENDPOINT_TRANSFORM_CONTEXT_VARIABLE, header("iko_endpointTransformContext"))
            .removeHeaders("iko_*", "iko_trace_id")
            .process { exchange ->
                val aggregatedDataProfileName = exchange.getVariable("profile", String::class.java)
                val aggregatedDataProfile = aggregatedDataProfileRepository.findByName(aggregatedDataProfileName)
                if (aggregatedDataProfile == null) {
                    throw IllegalArgumentException("AggregatedDataProfile with name '$aggregatedDataProfileName' not found")
                }
                exchange.setVariable("aggregatedDataProfileId", aggregatedDataProfile.id)
            }
            .routeDescription("REST consumer --> ADP entrypoint")
            .toD("direct:aggregated_data_profile_\${variable.aggregatedDataProfileId}")
    }

    companion object {
        const val ENDPOINT_TRANSFORM_CONTEXT_VARIABLE = "endpointTransformContext"
        const val ENDPOINT_TRANSFORM_RESULT_VARIABLE = "endpointTransformResult"
    }
}