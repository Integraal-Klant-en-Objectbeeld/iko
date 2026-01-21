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

package com.ritense.iko.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.security.oauth2.server.resource.authentication.ExpressionJwtGrantedAuthoritiesConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain

@EnableWebSecurity
@Configuration
class SecurityConfig {
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    fun actuatorSecurityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter,
    ): SecurityFilterChain {
        http
            .securityMatcher("/actuator/**")
            .oauth2Login { oauth2 -> oauth2.disable() }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }.sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info")
                    .permitAll()
                    .anyRequest()
                    .hasAnyAuthority("ROLE_ADMIN")
            }

        return http.build()
    }

    @Order(Ordered.LOWEST_PRECEDENCE - 1000)
    @Bean
    fun apiSecurityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter,
    ): SecurityFilterChain {
        http
            .securityMatcher(
                "/endpoints/**",
                "/aggregated-data-profiles/**",
            ).oauth2Login { oauth2 -> oauth2.disable() }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }.sessionManagement { session ->
                session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.authorizeHttpRequests { authorize ->
                authorize.anyRequest().authenticated()
            }

        return http.build()
    }

    @Bean
    fun jwtAuthenticationConverter(
        @Value("\${spring.security.oauth2.resourceserver.jwt.authority-prefix}") prefix: String,
        @Value("\${spring.security.oauth2.resourceserver.jwt.authorities-claim-name}") claimName: String,
    ): JwtAuthenticationConverter {
        val grantedAuthoritiesConverter =
            ExpressionJwtGrantedAuthoritiesConverter(SpelExpressionParser().parseRaw(claimName))
        grantedAuthoritiesConverter.setAuthorityPrefix(prefix)
        val jwtAuthenticationConverter = JwtAuthenticationConverter()
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter)
        return jwtAuthenticationConverter
    }

    @Bean
    fun oidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository: ClientRegistrationRepository) = OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository).apply {
        setPostLogoutRedirectUri("{baseUrl}/admin")
    }

    @Order(Ordered.LOWEST_PRECEDENCE - 100)
    @Bean
    fun adminSecurityFilterChain(
        http: HttpSecurity,
        oidcClientInitiatedLogoutSuccessHandler: OidcClientInitiatedLogoutSuccessHandler,
    ): SecurityFilterChain {
        http
            .securityMatcher("/admin/**", "/oauth2/**", "/login/**", "/logout/**")
            .oauth2Login { login ->
                login.userInfoEndpoint { user ->
                    user.oidcUserService(OidcUserService().apply { setRetrieveUserInfo { true } })
                    user.userAuthoritiesMapper { authorities ->
                        authorities
                            .filter { it::class == OidcUserAuthority::class }
                            .map { oidcUserAuthority -> oidcUserAuthority as OidcUserAuthority }
                            .map { oidcUserAuthority -> oidcUserAuthority.idToken }
                            .flatMap { oidcIdToken -> oidcIdToken.getClaimAsStringList("roles") }
                            .map { SimpleGrantedAuthority(it) }
                    }
                }
            }.logout { logout ->
                logout
                    .logoutSuccessHandler(oidcClientInitiatedLogoutSuccessHandler)
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
            }.authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/admin/**")
                    .hasAnyAuthority("ROLE_ADMIN")
                    .requestMatchers("/oauth2/**", "/login/**", "/logout")
                    .permitAll()
            }.csrf {
                it.disable()
            }

        return http.build()
    }

    @Bean
    fun authRoute() = AuthRoute()
}