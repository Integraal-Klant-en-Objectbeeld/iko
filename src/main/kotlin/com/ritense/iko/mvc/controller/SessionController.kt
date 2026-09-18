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

package com.ritense.iko.mvc.controller

import com.ritense.iko.security.AdminSessionProperties
import com.ritense.iko.security.AdminSessionTimeoutResolver
import com.ritense.iko.security.SessionTimeout
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * Keep-alive endpoint backing the "Continue" action of the session timeout
 * modal. The endpoint lives under `/admin`, so it is covered by the admin
 * filter chain and requires an authenticated session.
 *
 * Each ping performs a real Keycloak refresh-token grant (rotating the stored
 * tokens and sliding the SSO idle timeout), reads the fresh `refresh_expires_in`
 * straight from the token response to align the servlet
 * [jakarta.servlet.http.HttpSession] inactivity timer (capped at the static
 * config max), and returns the recomputed [SessionTimeout] as JSON.
 *
 * Unlike an `OAuth2AuthorizedClientManager` (which only refreshes when the
 * *access* token is expired and would otherwise return the stale client), this
 * always exchanges the refresh token, so a session whose refresh token has been
 * revoked or expired at Keycloak is detected within one ping: the grant fails
 * with an [OAuth2AuthorizationException] and the ping returns `401`, driving the
 * browser to log out instead of extending a dead session.
 */
@Controller
@RequestMapping("/admin/session")
internal class SessionController(
    private val refreshTokenResponseClient: OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest>,
    private val authorizedClientService: OAuth2AuthorizedClientService,
    private val adminSessionTimeoutResolver: AdminSessionTimeoutResolver,
    private val adminSessionProperties: AdminSessionProperties,
) {
    @GetMapping("/ping")
    fun ping(
        request: HttpServletRequest,
        authentication: Authentication,
    ): ResponseEntity<SessionTimeout> {
        val authorizedClient = authorizedClientService
            .loadAuthorizedClient<OAuth2AuthorizedClient>(REGISTRATION_ID, authentication.name)
        val refreshToken = authorizedClient?.refreshToken
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        // Force a refresh-token grant. A revoked/expired refresh token fails
        // here with an OAuth2AuthorizationException, meaning the session is dead.
        val tokenResponse = try {
            refreshTokenResponseClient.getTokenResponse(
                OAuth2RefreshTokenGrantRequest(
                    authorizedClient.clientRegistration,
                    authorizedClient.accessToken,
                    refreshToken,
                ),
            )
        } catch (_: OAuth2AuthorizationException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        // Persist the rotated tokens so subsequent requests use the new ones.
        // Keycloak may omit a new refresh token; keep the current one if so.
        authorizedClientService.saveAuthorizedClient(
            OAuth2AuthorizedClient(
                authorizedClient.clientRegistration,
                authentication.name,
                tokenResponse.accessToken,
                tokenResponse.refreshToken ?: refreshToken,
            ),
            authentication,
        )

        // Seconds until forced re-login, straight from Keycloak's non-standard
        // refresh_expires_in. A non-positive/absent value means the refresh
        // token is already dead, so log out rather than extend the session.
        val timeoutSeconds = tokenResponse.refreshExpiresInSeconds()
        if (timeoutSeconds <= 0) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        // Align the servlet session idle timer with the token-derived timeout,
        // capped so it never exceeds the static config max.
        val session = request.getSession(false)
        session?.maxInactiveInterval = timeoutSeconds
            .coerceAtMost(adminSessionProperties.timeoutSeconds)
            .toInt()

        return ResponseEntity.ok(adminSessionTimeoutResolver.resolve(session))
    }

    private fun OAuth2AccessTokenResponse.refreshExpiresInSeconds(): Long = additionalParameters[REFRESH_EXPIRES_IN]
        ?.toString()
        ?.toLongOrNull()
        ?: -1

    companion object {
        private const val REGISTRATION_ID = "keycloak"
        private const val REFRESH_EXPIRES_IN = "refresh_expires_in"
    }
}