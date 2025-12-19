package com.ritense.iko.aggregateddataprofile.camel

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.CONTAINER_PARAM_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.ENDPOINT_TRANSFORM_CONTEXT_VARIABLE
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
            .name(CONTAINER_PARAM_HEADER)
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
                    ?: throw IllegalArgumentException("AggregatedDataProfile with name '$aggregatedDataProfileName' not found")
                exchange.setVariable("aggregatedDataProfileId", aggregatedDataProfile.id)
            }
            .routeDescription("REST consumer --> ADP entrypoint")
            .toD("direct:aggregated_data_profile_\${variable.aggregatedDataProfileId}")
    }
}