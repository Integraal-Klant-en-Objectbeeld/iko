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
import jakarta.servlet.http.HttpSession
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import java.time.Duration

class SessionControllerTest {
    private val authorizedClientManager = mock<OAuth2AuthorizedClientManager>()
    private val adminSessionTimeoutResolver = mock<AdminSessionTimeoutResolver>()
    private val adminSessionProperties = AdminSessionProperties(
        timeout = Duration.ofMinutes(30),
        warningBefore = Duration.ofMinutes(2),
    )
    private val authentication = mock<Authentication>()

    private val controller = SessionController(
        authorizedClientManager = authorizedClientManager,
        adminSessionTimeoutResolver = adminSessionTimeoutResolver,
        adminSessionProperties = adminSessionProperties,
    )

    @Test
    fun `ping refreshes the token, aligns the session and returns the resolved timeout`() {
        val authorizedClient = mock<OAuth2AuthorizedClient>()
        whenever(authorizedClientManager.authorize(any<OAuth2AuthorizeRequest>())).thenReturn(authorizedClient)
        val timeout = SessionTimeout(timeoutSeconds = 600, warningSeconds = 120)
        whenever(adminSessionTimeoutResolver.resolve(authentication)).thenReturn(timeout)
        val session = mock<HttpSession>()
        val request = mock<HttpServletRequest>()
        whenever(request.getSession(false)).thenReturn(session)

        val response = controller.ping(request, authentication)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(timeout)
        // 600s is below the 1800s static cap, so it is applied as-is.
        verify(session).maxInactiveInterval = 600
    }

    @Test
    fun `ping caps the servlet session interval at the static maximum`() {
        val authorizedClient = mock<OAuth2AuthorizedClient>()
        whenever(authorizedClientManager.authorize(any<OAuth2AuthorizeRequest>())).thenReturn(authorizedClient)
        // Token-derived timeout exceeds the static 1800s max.
        whenever(adminSessionTimeoutResolver.resolve(authentication))
            .thenReturn(SessionTimeout(timeoutSeconds = 36000, warningSeconds = 120))
        val session = mock<HttpSession>()
        val request = mock<HttpServletRequest>()
        whenever(request.getSession(false)).thenReturn(session)

        controller.ping(request, authentication)

        verify(session).maxInactiveInterval = 1800
    }

    @Test
    fun `ping returns 401 and does not resolve a timeout when the refresh fails`() {
        whenever(authorizedClientManager.authorize(any<OAuth2AuthorizeRequest>())).thenReturn(null)
        val request = mock<HttpServletRequest>()

        val response = controller.ping(request, authentication)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body).isNull()
        verify(adminSessionTimeoutResolver, never()).resolve(any())
        verify(request, never()).getSession(eq(false))
    }
}