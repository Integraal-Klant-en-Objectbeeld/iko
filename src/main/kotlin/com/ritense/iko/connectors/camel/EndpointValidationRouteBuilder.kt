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

import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_ENDPOINT_ID_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_ID_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_INSTANCE_ID_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_INSTANCE_TAG_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_OPERATION_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_TAG_VARIABLE
import com.ritense.iko.camel.IkoRouteHelper
import com.ritense.iko.camel.IkoRouteHelper.Companion.GLOBAL_ERROR_HANDLER_CONFIGURATION
import com.ritense.iko.connectors.exception.EndpointValidationFailed
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import org.apache.camel.builder.RouteBuilder
import java.util.UUID

class EndpointValidationRouteBuilder(
    val connectorEndpointRepository: ConnectorEndpointRepository,
    val connectorInstanceRepository: ConnectorInstanceRepository,
) : RouteBuilder() {
    override fun configure() {
        from(IkoRouteHelper.endpoint("validate"))
            .routeId("endpoint-validation")
            .routeConfigurationId(GLOBAL_ERROR_HANDLER_CONFIGURATION)
            .process { ex ->
                val connectorId = ex.getVariable(CONNECTOR_ID_VARIABLE, UUID::class.java)
                val connectorTag = ex.getVariable(CONNECTOR_TAG_VARIABLE, String::class.java)
                val connectorInstanceTag = ex.getVariable(CONNECTOR_INSTANCE_TAG_VARIABLE, String::class.java)
                val operation = ex.getVariable(CONNECTOR_OPERATION_VARIABLE, String::class.java)

                val connectorEndpoint =
                    connectorEndpointRepository.findByConnectorIdAndOperation(connectorId, operation)
                        ?: throw EndpointValidationFailed("Unknown operation: [$connectorTag.$operation] on connector with id: $connectorId")
                val connectorInstance =
                    connectorInstanceRepository.findByConnectorIdAndTag(connectorId, connectorInstanceTag)
                        ?: throw EndpointValidationFailed("Unknown instance: [$connectorTag.$connectorInstanceTag] on connector with id: $connectorId")

                ex.setVariable(CONNECTOR_ENDPOINT_ID_VARIABLE, connectorEndpoint.id)
                ex.setVariable(CONNECTOR_INSTANCE_ID_VARIABLE, connectorInstance.id)
            }
    }
}