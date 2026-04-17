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

package com.ritense.iko.connectors.camel

import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_OPERATION_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_TAG_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_VERSION_VARIABLE
import com.ritense.iko.camel.IkoRouteHelper
import com.ritense.iko.camel.IkoRouteHelper.Companion.GLOBAL_ERROR_HANDLER_CONFIGURATION
import org.apache.camel.builder.RouteBuilder

class TransformDispatcherRouteBuilder : RouteBuilder() {
    override fun configure() {
        from(IkoRouteHelper.transform())
            .routeId("transform-dispatcher")
            .routeConfigurationId(GLOBAL_ERROR_HANDLER_CONFIGURATION)
            .choice()
            .`when` { exchange ->
                exchange.context.hasEndpoint(
                    IkoRouteHelper.transform("${exchange.getVariable(CONNECTOR_TAG_VARIABLE)}:${exchange.getVariable(CONNECTOR_VERSION_VARIABLE)}"),
                ) != null
            }
            .toD(IkoRouteHelper.transform("\${variable.${CONNECTOR_TAG_VARIABLE}}:\${variable.${CONNECTOR_VERSION_VARIABLE}}"))
            .end()
            .choice()
            .`when` { exchange ->
                exchange.context.hasEndpoint(
                    IkoRouteHelper.transform(
                        "${exchange.getVariable(CONNECTOR_TAG_VARIABLE)}:${exchange.getVariable(CONNECTOR_VERSION_VARIABLE)}.${exchange.getVariable(CONNECTOR_OPERATION_VARIABLE)}",
                    ),
                ) != null
            }
            .toD(IkoRouteHelper.transform("\${variable.${CONNECTOR_TAG_VARIABLE}}:\${variable.${CONNECTOR_VERSION_VARIABLE}}.\${variable.${CONNECTOR_OPERATION_VARIABLE}}"))
            .end()
    }
}