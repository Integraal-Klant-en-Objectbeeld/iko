package com.ritense.iko.aggregateddataprofile.camel

import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.ADP_CONTAINER_PARAM_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.ADP_ENDPOINT_TRANSFORM_CONTEXT_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.ADP_ID_PARAM_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.ADP_PROFILE_NAME_PARAM_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Variables.ENDPOINT_TRANSFORM_CONTEXT_VARIABLE
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Variables.IKO_CORRELATION_ID_VARIABLE
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Variables.IKO_TRACE_ID_VARIABLE
import com.ritense.iko.aggregateddataprofile.error.AggregatedDataProfileNotFound
import com.ritense.iko.aggregateddataprofile.error.AggregatedDataProfileQueryParametersError
import com.ritense.iko.aggregateddataprofile.error.errorResponse
import com.ritense.iko.aggregateddataprofile.processor.ContainerParamsProcessor
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.ParamDefinition
import org.apache.camel.model.rest.RestParamType.query
import org.springframework.http.HttpStatus

class AggregatedDataProfileRoute(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val containerParamsProcessor: ContainerParamsProcessor,
) : RouteBuilder() {
    override fun configure() {
        val containerParamsParamDefinition = ParamDefinition()
            .name(ADP_CONTAINER_PARAM_HEADER)
            .description("Container parameters for ADP")
            .type(query)
            .arrayType("String")
            .required(false)
        val ikoIdParamDefinition = ParamDefinition()
            .name(ADP_ID_PARAM_HEADER)
            .description("Id to use with ADPs that require one")
            .type(query)
            .dataType("String")
            .required(false)

        onException(AggregatedDataProfileNotFound::class.java)
            .errorResponse(status = HttpStatus.NOT_FOUND)

        onException(AggregatedDataProfileQueryParametersError::class.java)
            .errorResponse(status = HttpStatus.BAD_REQUEST, exposeMessage = true)

        rest("/aggregated-data-profiles")
            .description("Resolve ADP by profile name")
            .get("/{$ADP_PROFILE_NAME_PARAM_HEADER}")
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
            .setVariable(IKO_CORRELATION_ID_VARIABLE, simple("\${exchangeId}"))
            .setVariable(IKO_TRACE_ID_VARIABLE, header(IKO_TRACE_ID_VARIABLE))
            .setVariable("profile", header(ADP_PROFILE_NAME_PARAM_HEADER))
            .setVariable(ENDPOINT_TRANSFORM_CONTEXT_VARIABLE, header(ADP_ENDPOINT_TRANSFORM_CONTEXT_HEADER))
            .removeHeaders("adp_*")
            .process { exchange ->
                val aggregatedDataProfileName = exchange.getVariable("profile", String::class.java)
                val aggregatedDataProfile = aggregatedDataProfileRepository.findByName(aggregatedDataProfileName)
                if (aggregatedDataProfile == null) {
                    throw AggregatedDataProfileNotFound(aggregatedDataProfileName)
                } else {
                    exchange.setVariable("aggregatedDataProfileId", aggregatedDataProfile.id)
                }
            }
            .routeDescription("REST consumer --> ADP entrypoint")
            .toD("direct:aggregated_data_profile_\${variable.aggregatedDataProfileId}")
    }
}