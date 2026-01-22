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