package com.ritense.iko.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import kotlin.math.log

@Configuration
class SecurityConfig {

    @Order(Ordered.LOWEST_PRECEDENCE)
    @Bean
    fun adminSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .oauth2Login(Customizer.withDefaults())
            .logout {
                    logout ->
                logout.invalidateHttpSession(true).clearAuthentication(true).logoutSuccessUrl("/admin")
            }
            .authorizeHttpRequests { authorize ->
                authorize.requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "SCOPE_openid")
                    .requestMatchers("/oauth2/**", "/login/**", "/logout").permitAll()
                    .anyRequest().denyAll()
            }

        return http.build()
    }

}