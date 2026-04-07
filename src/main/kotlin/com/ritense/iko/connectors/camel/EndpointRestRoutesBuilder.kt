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

import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_INSTANCE_TAG_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_OPERATION_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_TAG_VARIABLE
import com.ritense.iko.camel.IkoRouteHelper
import com.ritense.iko.camel.IkoRouteHelper.Companion.GLOBAL_ERROR_HANDLER_CONFIGURATION
import com.ritense.iko.connectors.processor.ConnectorLookupProcessor
import org.apache.camel.builder.RouteBuilder

class EndpointRestRoutesBuilder(
    private val connectorLookup: ConnectorLookupProcessor,
) : RouteBuilder() {

    override fun configure() {
        rest("/endpoints")
            .get("/{iko_connector_tag}/{iko_connector_instance_tag}/{iko_operation}")
            .to(IkoRouteHelper.iko("rest:endpoint"))
            .get("/{iko_connector_tag}/{iko_connector_instance_tag}/{iko_operation}/{id}")
            .to(IkoRouteHelper.iko("rest:endpoint.id"))

        from(IkoRouteHelper.iko("rest:endpoint"))
            .routeId("rest-endpoint")
            .routeConfigurationId(GLOBAL_ERROR_HANDLER_CONFIGURATION)
            .setVariable(CONNECTOR_TAG_VARIABLE, header("iko_connector_tag"))
            .setVariable(CONNECTOR_INSTANCE_TAG_VARIABLE, header("iko_connector_instance_tag"))
            .setVariable(CONNECTOR_OPERATION_VARIABLE, header("iko_operation"))
            .process(connectorLookup)
            .removeHeaders("iko_*")
            .to(IkoRouteHelper.endpoint("validate"))
            .to(IkoRouteHelper.endpoint("auth"))
            .to(IkoRouteHelper.iko("config"))
            .to(IkoRouteHelper.transform())
            .to(IkoRouteHelper.connector())
            .marshal()
            .json()

        from(IkoRouteHelper.iko("rest:endpoint.id"))
            .routeId("rest-endpoint-id")
            .routeConfigurationId(GLOBAL_ERROR_HANDLER_CONFIGURATION)
            .setVariable(CONNECTOR_TAG_VARIABLE, header("iko_connector_tag"))
            .setVariable(CONNECTOR_INSTANCE_TAG_VARIABLE, header("iko_connector_instance_tag"))
            .setVariable(CONNECTOR_OPERATION_VARIABLE, header("iko_operation"))
            .setVariable("id", header("id"))
            .process(connectorLookup)
            .removeHeaders("iko_*")
            .to(IkoRouteHelper.endpoint("validate"))
            .to(IkoRouteHelper.endpoint("auth"))
            .to(IkoRouteHelper.iko("config"))
            .to(IkoRouteHelper.transform())
            .toD(IkoRouteHelper.connector())
            .marshal()
            .json()
    }
}