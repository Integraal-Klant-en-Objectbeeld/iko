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

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.util.Base64

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
 * The timeout is the number of seconds remaining on the refresh token, which
 * represents "time until forced re-login". The warning window is derived as
 * `min(configured warning-before, timeout / 2)` so the JS `timeoutSec >
 * warningSec` guard always holds.
 *
 * Spring's default token-response converter only populates the *access* token
 * expiry (`expires_in`); it ignores Keycloak's non-standard `refresh_expires_in`,
 * so [OAuth2RefreshToken.getExpiresAt] is virtually always `null`. Keycloak
 * refresh tokens are JWTs carrying an `exp` claim, so when the stored expiry is
 * missing this reads `exp` straight from the token payload.
 *
 * If the authorized client, refresh token or its expiry cannot be read (no
 * client, opaque/unparseable token, missing `exp`), this falls back to the
 * static [AdminSessionProperties] defaults so an admin is never locked out by a
 * transient store issue.
 */
@Component
internal class AdminSessionTimeoutResolver(
    private val authorizedClientService: OAuth2AuthorizedClientService,
    private val adminSessionProperties: AdminSessionProperties,
    private val clock: Clock = Clock.systemUTC(),
    private val objectMapper: ObjectMapper = ObjectMapper(),
) {
    fun resolve(authentication: Authentication): SessionTimeout {
        val refreshToken = authorizedClientService
            .loadAuthorizedClient<OAuth2AuthorizedClient>(REGISTRATION_ID, authentication.name)
            ?.refreshToken

        val expiresAt = refreshToken?.expiresAt
            ?: refreshToken?.tokenValue?.let { refreshTokenJwtExpiry(it) }

        val timeoutSeconds = expiresAt
            ?.let { (it.epochSecond - clock.instant().epochSecond).coerceAtLeast(0) }
            ?: adminSessionProperties.timeoutSeconds

        val warningSeconds = minOf(adminSessionProperties.warningBeforeSeconds, timeoutSeconds / 2)

        return SessionTimeout(timeoutSeconds, warningSeconds)
    }

    /**
     * Reads the `exp` claim (epoch seconds) from a Keycloak refresh-token JWT
     * without verifying the signature: the token is held server-side and was
     * issued by Keycloak over TLS, so this is only extracting the expiry already
     * decided by the realm. Returns `null` for any non-JWT/opaque token, a
     * malformed payload or a missing/non-numeric `exp`, driving the static
     * fallback.
     */
    private fun refreshTokenJwtExpiry(tokenValue: String): Instant? = runCatching {
        val parts = tokenValue.split(".")
        if (parts.size < 2) return null
        val payload = Base64.getUrlDecoder().decode(parts[1])
        objectMapper.readTree(payload)
            .get("exp")
            ?.takeIf { it.isNumber }
            ?.let { Instant.ofEpochSecond(it.asLong()) }
    }.getOrNull()

    companion object {
        private const val REGISTRATION_ID = "keycloak"
    }
}