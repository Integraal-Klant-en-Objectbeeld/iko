package com.ritense.iko.profile

import org.apache.camel.CamelContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RelationsConfig(
    private val camelContext: CamelContext,
    private val profileRepository: ProfileRepository
) {

    init {
        this.profileRepository.findAll().forEach { profile ->
            camelContext.addRoutes(ProfileRouteBuilder(camelContext, profile))
        }
    }

    @Bean
    fun profileRoute() = ProfileRoute(profileRepository)

}


