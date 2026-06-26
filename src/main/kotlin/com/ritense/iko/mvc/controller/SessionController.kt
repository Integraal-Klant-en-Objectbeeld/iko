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
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * Keep-alive endpoint backing the "Continue" action of the session timeout
 * modal. The endpoint lives under `/admin`, so it is covered by the admin
 * filter chain and requires an authenticated session.
 *
 * Each ping refreshes the user's Keycloak token through the
 * [OAuth2AuthorizedClientManager] (sliding the SSO idle timeout), re-resolves
 * the session timeout from the new `refresh_expires_in`, aligns the servlet
 * [jakarta.servlet.http.HttpSession] inactivity timer (capped at the static
 * config max) and returns the recomputed [SessionTimeout] as JSON. If the
 * refresh fails, the session is effectively dead and the ping returns `401`,
 * driving the browser to log out.
 */
@Controller
@RequestMapping("/admin/session")
internal class SessionController(
    private val authorizedClientManager: OAuth2AuthorizedClientManager,
    private val adminSessionTimeoutResolver: AdminSessionTimeoutResolver,
    private val adminSessionProperties: AdminSessionProperties,
) {
    @GetMapping("/ping")
    fun ping(
        request: HttpServletRequest,
        authentication: Authentication,
    ): ResponseEntity<SessionTimeout> {
        val authorizeRequest = OAuth2AuthorizeRequest
            .withClientRegistrationId(REGISTRATION_ID)
            .principal(authentication)
            .build()

        // A failed/absent refresh means the Keycloak session is dead; log out.
        authorizedClientManager.authorize(authorizeRequest)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val timeout = adminSessionTimeoutResolver.resolve(authentication)

        // Align the servlet session idle timer with the token-derived timeout,
        // capped so it never exceeds the static config max.
        request.getSession(false)?.maxInactiveInterval =
            timeout.timeoutSeconds
                .coerceAtMost(adminSessionProperties.timeoutSeconds)
                .toInt()

        return ResponseEntity.ok(timeout)
    }

    companion object {
        private const val REGISTRATION_ID = "keycloak"
    }
}