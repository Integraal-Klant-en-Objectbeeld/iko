package com.ritense.iko.aggregateddataprofile.camel

import com.ritense.iko.aggregateddataprofile.error.AggregatedDataProfileAccessDenied
import org.apache.camel.builder.RouteBuilder
import org.springframework.security.core.context.SecurityContextHolder

class AuthRoute : RouteBuilder() {
    override fun configure() {
        from("direct:auth")
            .routeId("authenticate")
            .routeConfigurationId("global-error-handler-configuration")
            .process { ex ->
                val exAuthorities = ex.getVariable("authorities", List::class.java)
                exAuthorities?.let {
                    if (it.isEmpty()) {
                        return@process
                    }

                    if (SecurityContextHolder.getContext().authentication != null &&
                        SecurityContextHolder.getContext().authentication.authorities.any { x ->
                            it.contains(x.authority)
                        }
                    ) {
                        return@process
                    }

                    throw AggregatedDataProfileAccessDenied("User is not authorized to access this route. Missing authorities: $exAuthorities")
                }
                throw AggregatedDataProfileAccessDenied("User is not authorized to access this route")
            }
    }
}