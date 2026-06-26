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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class AdminSessionTimeoutResolverTest {
    private val now = Instant.parse("2026-06-26T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val authorizedClientService = mock<OAuth2AuthorizedClientService>()
    private val adminSessionProperties = AdminSessionProperties(
        timeout = Duration.ofMinutes(30),
        warningBefore = Duration.ofMinutes(2),
    )
    private val authentication = mock<Authentication>().also {
        whenever(it.name).thenReturn("admin")
    }

    private val resolver = AdminSessionTimeoutResolver(
        authorizedClientService = authorizedClientService,
        adminSessionProperties = adminSessionProperties,
        clock = clock,
    )

    @Test
    fun `derives timeout from refresh-token expiry and caps warning at half the timeout when shorter than configured`() {
        // Refresh token expires in 200 seconds; configured warning-before is 120s,
        // but timeout/2 = 100s is shorter, so warning is capped at 100s.
        stubRefreshTokenExpiringAt(now.plusSeconds(200))

        val result = resolver.resolve(authentication)

        assertThat(result.timeoutSeconds).isEqualTo(200)
        assertThat(result.warningSeconds).isEqualTo(100)
    }

    @Test
    fun `keeps the configured warning-before when it is shorter than half the timeout`() {
        // Refresh token expires in 1800 seconds; timeout/2 = 900s, configured 120s is shorter.
        stubRefreshTokenExpiringAt(now.plusSeconds(1800))

        val result = resolver.resolve(authentication)

        assertThat(result.timeoutSeconds).isEqualTo(1800)
        assertThat(result.warningSeconds).isEqualTo(120)
    }

    @Test
    fun `clamps a past expiry to zero`() {
        stubRefreshTokenExpiringAt(now.minusSeconds(60))

        val result = resolver.resolve(authentication)

        assertThat(result.timeoutSeconds).isEqualTo(0)
        assertThat(result.warningSeconds).isEqualTo(0)
    }

    @Test
    fun `falls back to static defaults when no authorized client is found`() {
        stubAuthorizedClient(null)

        val result = resolver.resolve(authentication)

        assertThat(result.timeoutSeconds).isEqualTo(1800)
        assertThat(result.warningSeconds).isEqualTo(120)
    }

    @Test
    fun `falls back to static defaults when the refresh token is absent`() {
        val client = mock<OAuth2AuthorizedClient>()
        whenever(client.refreshToken).thenReturn(null)
        stubAuthorizedClient(client)

        val result = resolver.resolve(authentication)

        assertThat(result.timeoutSeconds).isEqualTo(1800)
        assertThat(result.warningSeconds).isEqualTo(120)
    }

    @Test
    fun `falls back to static defaults when the refresh token has no expiry`() {
        val client = mock<OAuth2AuthorizedClient>()
        whenever(client.refreshToken).thenReturn(OAuth2RefreshToken("refresh-token-value", now, null))
        stubAuthorizedClient(client)

        val result = resolver.resolve(authentication)

        assertThat(result.timeoutSeconds).isEqualTo(1800)
        assertThat(result.warningSeconds).isEqualTo(120)
    }

    private fun stubRefreshTokenExpiringAt(expiresAt: Instant) {
        // issuedAt must precede expiresAt per the OAuth2RefreshToken contract; pick a
        // point just before the expiry so the "past expiry" case can also be modelled.
        val issuedAt = expiresAt.minusSeconds(1)
        val client = mock<OAuth2AuthorizedClient>()
        whenever(client.refreshToken).thenReturn(OAuth2RefreshToken("refresh-token-value", issuedAt, expiresAt))
        stubAuthorizedClient(client)
    }

    private fun stubAuthorizedClient(client: OAuth2AuthorizedClient?) {
        whenever(
            authorizedClientService.loadAuthorizedClient<OAuth2AuthorizedClient>(eq("keycloak"), eq("admin")),
        ).thenReturn(client)
    }
}