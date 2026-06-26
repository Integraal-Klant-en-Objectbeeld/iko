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

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.stereotype.Component
import java.time.Clock

/**
 * Per-session admin UI timeout, in seconds. The countdown shown by the
 * session-timeout modal is derived from these values.
 */
internal data class SessionTimeout(
    val timeoutSeconds: Long,
    val warningSeconds: Long,
)

/**
 * Resolves the admin UI session timeout from the logged-in user's Keycloak
 * refresh-token expiry rather than the static [AdminSessionProperties] config.
 *
 * The timeout is the number of seconds remaining on the refresh token
 * (`refresh_expires_in`), which represents "time until forced re-login". The
 * warning window is derived as `min(configured warning-before, timeout / 2)` so
 * the JS `timeoutSec > warningSec` guard always holds.
 *
 * If the authorized client, refresh token or its expiry cannot be read, this
 * falls back to the static [AdminSessionProperties] defaults so an admin is
 * never locked out by a transient store issue.
 */
@Component
internal class AdminSessionTimeoutResolver(
    private val authorizedClientService: OAuth2AuthorizedClientService,
    private val adminSessionProperties: AdminSessionProperties,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun resolve(authentication: Authentication): SessionTimeout {
        val expiresAt = authorizedClientService
            .loadAuthorizedClient<OAuth2AuthorizedClient>(REGISTRATION_ID, authentication.name)
            ?.refreshToken
            ?.expiresAt

        val timeoutSeconds = expiresAt
            ?.let { (it.epochSecond - clock.instant().epochSecond).coerceAtLeast(0) }
            ?: adminSessionProperties.timeoutSeconds

        val warningSeconds = minOf(adminSessionProperties.warningBeforeSeconds, timeoutSeconds / 2)

        return SessionTimeout(timeoutSeconds, warningSeconds)
    }

    companion object {
        private const val REGISTRATION_ID = "keycloak"
    }
}