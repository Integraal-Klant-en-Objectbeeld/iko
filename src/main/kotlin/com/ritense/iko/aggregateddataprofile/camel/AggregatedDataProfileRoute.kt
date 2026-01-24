/*
 * Copyright (C) 2026 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritense.iko.aggregateddataprofile.camel

import com.ritense.iko.aggregateddataprofile.error.AggregatedDataProfileNotFound
import com.ritense.iko.aggregateddataprofile.processor.ContainerParamsProcessor
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.camel.IkoConstants.Headers.ADP_CONTAINER_PARAM_HEADER
import com.ritense.iko.camel.IkoConstants.Headers.ADP_ENDPOINT_TRANSFORM_CONTEXT_HEADER
import com.ritense.iko.camel.IkoConstants.Headers.ADP_ID_PARAM_HEADER
import com.ritense.iko.camel.IkoConstants.Headers.ADP_PROFILE_NAME_PARAM_HEADER
import com.ritense.iko.camel.IkoConstants.Variables.ENDPOINT_TRANSFORM_CONTEXT_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.IKO_CORRELATION_ID_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.IKO_TRACE_ID_VARIABLE
import com.ritense.iko.camel.IkoRouteHelper.Companion.GLOBAL_ERROR_HANDLER_CONFIGURATION
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.ParamDefinition
import org.apache.camel.model.rest.RestParamType

class AggregatedDataProfileRoute(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val containerParamsProcessor: ContainerParamsProcessor,
) : RouteBuilder() {
    override fun configure() {
        val profileNamePathParam = ParamDefinition()
            .type(RestParamType.path)
            .name(ADP_PROFILE_NAME_PARAM_HEADER)
            .dataType("string")
            .description("The profile name of the ADP that is being queried")
            .example("zaken")
            .required(true)

        val containerParamsParam = ParamDefinition()
            .name(ADP_CONTAINER_PARAM_HEADER)
            .description(
                "ContainerParam for use with the endpointTransform of an ADP " +
                    "or sourceToEndpointMapping of a Relation encoded as Base64",
            )
            .type(RestParamType.query)
            .arrayType("string")
            .dataType("string")
            .example(
                "Base64 encoded ContainerParam",
                "eyAiY29udGFpbmVySWQiOiAiemFrZW4iLCAicGFnZWFibGUiOiB7ICJwYWdlTnVtYmVyIjogIjAiLCAicGFnZV" +
                    "NpemUiOiAiMTAiLCAic29ydCI6IHsgIm9yZGVycyI6IFsgeyAicHJvcGVydHkiOiAiaWRlbnRpZmljYXRpZSIgImRpcmV" +
                    "jdGlvbiI6ICJBU0MiIH0gXSB9IH0sICJmaWx0ZXJzIjogeyAiemFha3R5cGUiOiAiaHR0cHM6Ly9leGFtcGxlLmNvbS9j" +
                    "YXRhbG9naS9hcGkvemFha3R5cGVuL2IyNzU4MWM3LTMxMTMtNDE5NS1hZGM2LTkxNTY1MTNiOWUyZSIgfSB9",
            )
            .required(false)
        val idParam = ParamDefinition()
            .name(ADP_ID_PARAM_HEADER)
            .description("Id to use with ADPs that require one")
            .type(RestParamType.query)
            .dataType("string")
            .required(false)

        rest("/aggregated-data-profiles")
            .description("Resolve ADP by profile name")
            .get("/{$ADP_PROFILE_NAME_PARAM_HEADER}")
            .routeId("get-aggregated-data-profile-rest-entrypoint")
            .param(profileNamePathParam)
            .param(idParam)
            .param(containerParamsParam)
            .to("direct:aggregated-data-profile-container-params")

        from("direct:aggregated-data-profile-container-params")
            .routeId("aggregated-data-profile-container-params")
            .routeConfigurationId(GLOBAL_ERROR_HANDLER_CONFIGURATION)
            .process {
                containerParamsProcessor.process(exchange = it)
            }
            .to("direct:aggregated_data_profile_rest_continuation")

        from("direct:aggregated_data_profile_rest_continuation")
            .routeId("aggregated-data-profile-rest-continuation")
            .routeConfigurationId(GLOBAL_ERROR_HANDLER_CONFIGURATION)
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