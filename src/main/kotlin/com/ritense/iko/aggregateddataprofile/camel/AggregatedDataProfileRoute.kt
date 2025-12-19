package com.ritense.iko.aggregateddataprofile.camel

import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.CONTAINER_PARAM_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.IKO_ENDPOINT_TRANSFORM_CONTEXT_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.IKO_ID_PARAM_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.IKO_PROFILE_PARAM_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.IKO_TRACE_ID_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Variables.ENDPOINT_TRANSFORM_CONTEXT_VARIABLE
import com.ritense.iko.aggregateddataprofile.processor.ContainerParamsProcessor
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.ParamDefinition
import org.apache.camel.model.rest.RestParamType.query

class AggregatedDataProfileRoute(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val containerParamsProcessor: ContainerParamsProcessor,
) : RouteBuilder() {
    override fun configure() {
        val containerParamsParamDefinition = ParamDefinition()
            .name(CONTAINER_PARAM_HEADER)
            .description("Container parameters for ADP")
            .type(query)
            .arrayType("String")
            .required(false)
        val ikoIdParamDefinition = ParamDefinition()
            .name(IKO_ID_PARAM_HEADER)
            .description("Id to use with ADPs that require one")
            .type(query)
            .dataType("String")
            .required(false)

        rest("/aggregated-data-profiles")
            .description("Resolve ADP by profile name")
            .get("/{$IKO_PROFILE_PARAM_HEADER}")
            .param(containerParamsParamDefinition)
            .param(ikoIdParamDefinition)
            .to("direct:aggregated-data-profile-container-params")

        from("direct:aggregated-data-profile-container-params")
            .routeId("aggregated-data-profile-container-params")
            .process {
                containerParamsProcessor.process(exchange = it)
            }
            .to("direct:aggregated_data_profile_rest_continuation")

        from("direct:aggregated_data_profile_rest_continuation")
            .routeId("aggregated-data-profile-rest-continuation")
            .setVariable("id", header(IKO_ID_PARAM_HEADER))
            .setVariable("profile", header(IKO_PROFILE_PARAM_HEADER))
            .setVariable(ENDPOINT_TRANSFORM_CONTEXT_VARIABLE, header(IKO_ENDPOINT_TRANSFORM_CONTEXT_HEADER))
            .removeHeaders("iko_*", IKO_TRACE_ID_HEADER)
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