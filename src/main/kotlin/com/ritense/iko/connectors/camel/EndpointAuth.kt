package com.ritense.iko.connectors.camel

import com.ritense.authzenk.AccessEvaluationApiRequest
import com.ritense.authzenk.Action
import com.ritense.authzenk.Client
import com.ritense.authzenk.Resource
import com.ritense.authzenk.Subject
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
    val pdpClient: Client
) : RouteBuilder() {
    override fun configure() {
        from("direct:iko:endpoint:auth")
            .routeId("endpoint-auth")
            .errorHandler(noErrorHandler())
            .process { ex ->
                val connectorEndpointId = ex.getVariable("connectorEndpointId", UUID::class.java)
                val connectorInstanceId = ex.getVariable("connectorInstanceId", UUID::class.java)

                val connectorEndpointRoles = connectorEndpointRoleRepository.findByConnectorEndpointAndConnectorInstance(
                    connectorEndpointRepository.getReferenceById(connectorEndpointId),
                    connectorInstanceRepository.getReferenceById(connectorInstanceId),
                )

                val decision = pdpClient.evaluationApi.evaluation(
                    AccessEvaluationApiRequest(
                        subject = Subject(
                            type = "subject",
                            id = "A"
                        ),
                        action = Action(
                            name = "can_read"
                        ),
                        resource = Resource(
                            type = "aggregated-data-profile",
                            id = "A"
                        ),
                    )
                )

                if (!decision.decision) {
                    throw AccessDeniedException("User is not authorized to perform access this route")
                }

            }
    }
}