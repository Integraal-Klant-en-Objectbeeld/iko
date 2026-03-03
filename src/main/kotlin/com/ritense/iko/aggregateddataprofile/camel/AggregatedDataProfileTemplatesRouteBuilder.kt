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
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.camel.IkoConstants.Headers
import com.ritense.iko.camel.IkoConstants.Variables
import com.ritense.iko.camel.IkoRouteHelper.Companion.GLOBAL_ERROR_HANDLER_CONFIGURATION
import org.apache.camel.builder.RouteBuilder

class AggregatedDataProfileTemplatesRouteBuilder(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
) : RouteBuilder() {
    override fun configure() {
        routeTemplate("aggregated-data-profile-entrypoint")
            .templateParameter("fromUri")
            .templateParameter("routeId")
            .templateParameter("routeDescription")
            .templateParameter("testRun", "false")
            .from("{{fromUri}}")
            .routeId("{{routeId}}")
            .routeDescription("{{routeDescription}}")
            .routeConfigurationId(GLOBAL_ERROR_HANDLER_CONFIGURATION)
            .setProperty("testRun").simple("{{testRun}}", Boolean::class.java)
            .choice()
            .`when`(simple("\${exchangeProperty.testRun} == false"))
            .to("direct:aggregated-data-profile-container-params")
            .end()
            .setVariable(Variables.PROFILE_NAME, header(Headers.ADP_PROFILE_NAME_PARAM_HEADER))
            .setVariable(Variables.PROFILE_VERSION, header(Headers.ADP_VERSION_PARAM_HEADER))
            .setVariable(Variables.ENDPOINT_TRANSFORM_CONTEXT_VARIABLE, header(Headers.ADP_ENDPOINT_TRANSFORM_CONTEXT_HEADER))
            .setVariable(Variables.IKO_CORRELATION_ID_VARIABLE, simple("\${exchangeId}"))
            .removeHeaders("adp_*")
            .process { exchange ->
                val aggregatedDataProfileName = exchange.getVariable(Variables.PROFILE_NAME, String::class.java)
                val aggregatedDataProfileVersion = exchange.getVariable(Variables.PROFILE_VERSION, String::class.java)
                val aggregatedDataProfile =
                    when (exchange.getProperty("testRun", Boolean::class.java)) {
                        true -> {
                            exchange.setVariable(Variables.IKO_TRACE_ID_VARIABLE, exchange.`in`.getHeader(Variables.IKO_TRACE_ID_VARIABLE))

                            aggregatedDataProfileRepository
                                .findByNameAndVersion(aggregatedDataProfileName, aggregatedDataProfileVersion)
                        }

                        else ->
                            aggregatedDataProfileRepository
                                .findByNameAndIsActiveTrue(aggregatedDataProfileName)
                    } ?: throw AggregatedDataProfileNotFound(aggregatedDataProfileName)

                exchange.setVariable("aggregatedDataProfileId", aggregatedDataProfile.id)
            }
            .toD("direct:aggregated_data_profile_\${variable.aggregatedDataProfileId}")
    }
}