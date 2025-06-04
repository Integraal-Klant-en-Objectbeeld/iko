package com.ritense.iko.profile

import com.ritense.iko.search.SearchRepository
import org.apache.camel.CamelContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RelationsConfig(
    private val camelContext: CamelContext,
    private val profileRepository: ProfileRepository,
    private val searchRepository: SearchRepository,
) {

    init {
        this.profileRepository.findAll().forEach { profile ->
            camelContext.addRoutes(ProfileRouteBuilder(camelContext, profile, searchRepository))
        }
    }

    @Bean
    fun profileRoute() = ProfileRoute(profileRepository)

}


