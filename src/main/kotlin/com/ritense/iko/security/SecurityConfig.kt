package com.ritense.iko.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.security.web.SecurityFilterChain

@EnableWebSecurity
@Configuration
class SecurityConfig {
    @Order(Ordered.LOWEST_PRECEDENCE - 1000)
    @Bean
    fun apiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher(
                "/endpoints/**",
                "/profile/**"
            )
            .oauth2Login { oauth2 -> oauth2.disable() }
            .oauth2ResourceServer { oauth2 -> oauth2.jwt(Customizer.withDefaults()) }
            .sessionManagement { session ->
                session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { authorize ->
                authorize.anyRequest().authenticated()
            }

        return http.build()
    }

    @Order(Ordered.LOWEST_PRECEDENCE - 100)
    @Bean
    fun adminSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/admin/**", "/oauth2/**", "/login/**", "/logout/**")
            .oauth2Login { login ->
                login.userInfoEndpoint { user ->
                    user.userAuthoritiesMapper { authorities ->
                        authorities.filter { it::class == OidcUserAuthority::class }
                            .map { oidcUserAuthority -> oidcUserAuthority as OidcUserAuthority }
                            .map { oidcUserAuthority -> oidcUserAuthority.idToken }
                            .flatMap { oidcIdToken -> oidcIdToken.getClaimAsStringList("roles") }
                            .map { SimpleGrantedAuthority(it) }
                    }
                }
            }
            .logout { logout ->
                logout.invalidateHttpSession(true).clearAuthentication(true).logoutSuccessUrl("/")
            }
            .authorizeHttpRequests { authorize ->
                authorize.requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN")
                    .requestMatchers("/oauth2/**", "/login/**", "/logout").permitAll()
            }

        return http.build()
    }

    @Bean
    fun authRoute() = AuthRoute()

}