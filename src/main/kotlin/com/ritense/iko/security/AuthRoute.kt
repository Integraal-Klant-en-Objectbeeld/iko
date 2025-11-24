package com.ritense.iko.security

import com.ritense.authzenk.AccessEvaluationApiRequest
import com.ritense.authzenk.Action
import com.ritense.authzenk.Client
import com.ritense.authzenk.Resource
import com.ritense.authzenk.Subject
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import org.apache.camel.builder.RouteBuilder
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder

class AuthRoute(val pdpClient: Client) : RouteBuilder() {

    override fun configure() {
        from("direct:auth")
            .routeId("authenticate")
            .errorHandler(noErrorHandler())
            .process { ex ->
                val aggregatedDataProfile = ex.getVariable("aggregatedDataProfile") as AggregatedDataProfile

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
                            type = "AggregatedDataProfile",
                            id = aggregatedDataProfile.id.toString(),
                        ),
                        context = mapOf(
                            "headers" to ex.getIn().headers.filter { !it.key.startsWith("Camel")},
                            "aggregatedDataProfile" to aggregatedDataProfile.name,
                            "aggregatedDataProfileRole" to aggregatedDataProfile.role
                        )
                    )
                )

                if (!decision.decision) {
                    throw AccessDeniedException("User is not authorized to perform access this route")
                }
            }
    }
}
