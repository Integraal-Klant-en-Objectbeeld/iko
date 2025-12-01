package com.ritense.iko.connectors.camel

import com.ritense.authzenk.AccessEvaluationApiRequest
import com.ritense.authzenk.Action
import com.ritense.authzenk.ActionSearchApiRequest
import com.ritense.authzenk.Client
import com.ritense.authzenk.PartialSubject
import com.ritense.authzenk.Resource
import com.ritense.authzenk.Subject
import com.ritense.authzenk.SubjectSearchApiRequest
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

                val connectorEndpoint = connectorEndpointRepository.findById(connectorEndpointId).orElseThrow {
                    throw Exception("Could not find connector")
                }

                val connectorInstance = connectorInstanceRepository.findById(connectorInstanceId).orElseThrow {
                    throw Exception("Could not find instance")
                }

                val decision = pdpClient.evaluationApi.evaluation(
                    AccessEvaluationApiRequest(
                        subject = Subject(
                            type = "User",
                            id = ex.getVariable("auth_token", String::class.java),
                        ),
                        action = Action(
                            name = "can_read",
                        ),
                        resource = Resource(
                            type = "Endpoint",
                            id = connectorEndpoint.id.toString(),
                        ),
                        context = mapOf(
                            "connector" to connectorInstance.connector.tag,
                            "connectorInstance" to connectorInstance.tag,
                            "connectorEndpoint" to connectorEndpoint.operation,
                            "connectorEndpointRoles" to connectorEndpointRoleRepository.findByConnectorEndpointAndConnectorInstance(
                                connectorEndpoint, connectorInstance
                            )
                        )
                    )
                )

                if (!decision.decision) {
                    throw AccessDeniedException("User is not authorized to perform access this route")
                }

            }
    }
}