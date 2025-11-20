package com.ritense.iko.security

import com.ritense.authzenk.AccessEvaluationApiRequest
import com.ritense.authzenk.Action
import com.ritense.authzenk.Client
import com.ritense.authzenk.Resource
import com.ritense.authzenk.Subject
import org.apache.camel.builder.RouteBuilder
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder

class AuthRoute(val pdpClient: Client) : RouteBuilder() {

    override fun configure() {
        from("direct:auth")
            .routeId("authenticate")
            .errorHandler(noErrorHandler())
            .process { ex ->
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
