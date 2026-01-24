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

import com.ritense.iko.camel.IkoRouteHelper.Companion.GLOBAL_ERROR_HANDLER_CONFIGURATION
import com.ritense.iko.connectors.error.ConnectorAccessDenied
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorEndpointRoleRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import org.apache.camel.builder.RouteBuilder
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

class EndpointAuth(
    val connectorEndpointRepository: ConnectorEndpointRepository,
    val connectorInstanceRepository: ConnectorInstanceRepository,
    val connectorEndpointRoleRepository: ConnectorEndpointRoleRepository,
) : RouteBuilder() {
    override fun configure() {
        from("direct:iko:endpoint:auth")
            .routeId("endpoint-auth")
            .routeConfigurationId(GLOBAL_ERROR_HANDLER_CONFIGURATION)
            .process { ex ->
                val connectorEndpointId = ex.getVariable("connectorEndpointId", UUID::class.java)
                val connectorInstanceId = ex.getVariable("connectorInstanceId", UUID::class.java)

                val connectorEndpointRoles =
                    connectorEndpointRoleRepository.findByConnectorEndpointAndConnectorInstance(
                        connectorEndpointRepository.getReferenceById(connectorEndpointId),
                        connectorInstanceRepository.getReferenceById(connectorInstanceId),
                    )

                connectorEndpointRoles.map { it.role }.toList().let {
                    log.debug("Authorizing endpoint with authority: {}", it)
                    if (it.isEmpty()) {
                        throw ConnectorAccessDenied("No roles defined for this endpoint.")
                    }

                    if (SecurityContextHolder.getContext().authentication != null &&
                        SecurityContextHolder.getContext().authentication.authorities.any { x ->
                            it.contains(x.authority)
                        }
                    ) {
                        return@process
                    }

                    throw ConnectorAccessDenied("User is not authorized to access this route. Missing authorities: $it")
                }
            }
    }
}