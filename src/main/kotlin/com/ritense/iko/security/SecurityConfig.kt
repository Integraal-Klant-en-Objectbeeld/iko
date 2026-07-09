/*
 * Copyright 2026 Den Haag, Ritense, Rotterdam, Utrecht, the Netherlands.
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

import com.ritense.iko.aggregateddataprofile.camel.AuthRoute
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
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest
import org.springframework.security.oauth2.client.endpoint.RestClientRefreshTokenTokenResponseClient
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.security.oauth2.server.resource.authentication.ExpressionJwtGrantedAuthoritiesConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.header.HeaderWriterFilter
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy
import org.springframework.security.web.util.matcher.AnyRequestMatcher
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher

internal const val ADMIN_CSP_TEMPLATE =
    "default-src 'self'; " +
        "script-src {nonce} 'strict-dynamic' https: 'self'; " +
        // 'unsafe-inline' is required: Carbon (Lit-based) web components apply
        // dynamic inline style="" attributes at runtime, and CSP nonces/hashes
        // do not apply to style attributes. script-src stays strict.
        "style-src 'self' 'unsafe-inline' https://1.www.s81c.com https://unpkg.com https://cdnjs.cloudflare.com; " +
        "img-src 'self' data:; " +
        "font-src 'self' https:; " +
        "connect-src 'self' https:; " +
        "worker-src 'self'; " +
        "object-src 'none'; " +
        "base-uri 'self'"

@EnableWebSecurity
@Configuration
class SecurityConfig {
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
        // Used when Keycloak RP-initiated logout can be performed (id token present).
        setPostLogoutRedirectUri("{baseUrl}/admin")
        // Fallback when there is no id token to build an end-session request
        // (e.g. the session/token already expired): without this the handler
        // redirects to "/" instead of the admin entry point.
        setDefaultTargetUrl("/admin")
    }

    @Order(Ordered.LOWEST_PRECEDENCE - 100)
    @Bean
    fun adminSecurityFilterChain(
        http: HttpSecurity,
        oidcClientInitiatedLogoutSuccessHandler: OidcClientInitiatedLogoutSuccessHandler,
        @Value("\${iko.security.admin.rolesClaim:roles}") adminRolesClaim: String,
        @Value("\${iko.security.admin.authorities:ROLE_ADMIN}") adminAuthorities: Array<String>,
        @Value("\${server.servlet.session.cookie.secure:false}") cookieSecure: Boolean,
    ): SecurityFilterChain {
        http
            .securityMatcher("/admin/**", "/oauth2/**", "/login/**", "/logout/**")
            .oauth2Login { login ->
                login.userInfoEndpoint { user ->
                    user.oidcUserService(OidcUserService().apply { setRetrieveUserInfo { true } })
                    user.userAuthoritiesMapper { authorities ->
                        authorities
                            .mapNotNull { (it as? OidcUserAuthority)?.idToken }
                            .flatMap { oidcIdToken -> oidcIdToken.getClaimAsStringList(adminRolesClaim) }
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
                    .hasAnyAuthority(*adminAuthorities)
                    .requestMatchers("/oauth2/**", "/login/**", "/logout")
                    .permitAll()
            }.csrf { csrf ->
                val repository =
                    CookieCsrfTokenRepository.withHttpOnlyFalse().apply {
                        setCookieCustomizer { cookie ->
                            cookie.sameSite("Lax")
                            cookie.secure(cookieSecure) // true in prod over HTTPS
                            cookie.path("/")
                        }
                    }
                csrf.csrfTokenRepository(repository)
                csrf.csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
            }.addFilterAfter(CsrfCookieFilter(), CsrfFilter::class.java)
            .addFilterBefore(CspNonceFilter(ADMIN_CSP_TEMPLATE), HeaderWriterFilter::class.java)
            .headers { headers ->
                headers.referrerPolicy { it.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN) }
                headers.httpStrictTransportSecurity { } // default: max-age + includeSubDomains, secure requests only
            }
            .exceptionHandling { ex ->
                ex.defaultAuthenticationEntryPointFor(
                    HtmxAuthenticationEntryPoint(),
                    RequestHeaderRequestMatcher("Hx-Request"),
                )
                ex.defaultAuthenticationEntryPointFor(
                    LoginUrlAuthenticationEntryPoint("/oauth2/authorization/keycloak"),
                    AnyRequestMatcher.INSTANCE,
                )
            }

        return http.build()
    }

    /**
     * Token-response client for the OAuth2 refresh-token grant. The session
     * keep-alive ping calls this directly (rather than going through an
     * [org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager],
     * which only refreshes when the *access* token is expired) so that every
     * ping performs a real refresh round-trip to Keycloak. That both slides the
     * SSO idle timeout and surfaces a revoked/expired refresh token as an
     * [org.springframework.security.oauth2.core.OAuth2AuthorizationException],
     * letting the ping log the admin out instead of silently extending a dead
     * session.
     */
    @Bean
    fun refreshTokenResponseClient(): OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> = RestClientRefreshTokenTokenResponseClient()

    @Bean
    fun authRoute() = AuthRoute()
}