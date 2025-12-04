package com.ritense.iko.connectors.camel

import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorEndpointRoleRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import org.apache.camel.builder.RouteBuilder
import org.springframework.security.access.AccessDeniedException
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
            .errorHandler(noErrorHandler())
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
                        throw AccessDeniedException("No roles defined for this endpoint.")
                    }

                    if (SecurityContextHolder.getContext().authentication != null &&
                        SecurityContextHolder.getContext().authentication.authorities.any { x ->
                            it.contains(x.authority)
                        }
                    ) {
                        return@process
                    }

                    throw AccessDeniedException("User is not authorized to perform access this route. Missing authorities: $it")
                }
            }
    }
}