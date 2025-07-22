package com.ritense.iko.security

import org.apache.camel.builder.RouteBuilder
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder

class AuthRoute() : RouteBuilder() {

    override fun configure() {
        from("direct:auth")
            .errorHandler(noErrorHandler())
            .process { ex ->
                ex.getVariable("authorities", List::class.java)?.let {
                    if (it.isEmpty()) {
                        return@process
                    }

                    if (SecurityContextHolder.getContext().authentication != null && SecurityContextHolder.getContext().authentication.authorities.any { x ->
                            it.contains(x.authority)
                        }) {
                        return@process
                    }
                }
                throw AccessDeniedException("User is not authorized to perform access this route")
            }
    }
}