package com.ritense.iko.source.bag

import org.apache.camel.component.spring.security.SpringSecurityAuthorizationPolicy
import org.apache.camel.spi.Policy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authorization.AuthorityAuthorizationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider


@Configuration
class Policies {
    @Bean
    fun adminPolicy(authenticationManager: AuthenticationManager): Policy {
        val policy = SpringSecurityAuthorizationPolicy()
        policy.authenticationManager = authenticationManager
        policy.authorizationManager = AuthorityAuthorizationManager.hasRole("ADMIN")
        policy.isUseThreadSecurityContext = true
        policy.isAlwaysReauthenticate = true
        return policy
    }

    @Bean
    fun authenticationManager(http: HttpSecurity, jwtDecoder: JwtDecoder): AuthenticationManager {
        return http
            .getSharedObject(AuthenticationManagerBuilder::class.java)
            .authenticationProvider(JwtAuthenticationProvider(jwtDecoder))
            .build()
    }

}