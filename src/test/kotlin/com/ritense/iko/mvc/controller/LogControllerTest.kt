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

import com.ritense.iko.logging.domain.LoggingEvent
import com.ritense.iko.logging.service.LoggingEventService
import com.ritense.iko.mvc.controller.HomeController.Companion.BASE_FRAGMENT_LOGGING
import com.ritense.iko.mvc.model.LoggingFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import java.time.Instant

class LogControllerTest {
    private val loggingEventService = mock<LoggingEventService>()
    private val controller = LogController(loggingEventService)

    private val emptyPage =
        PageImpl<LoggingEvent>(
            emptyList(),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "timestamp")),
            0,
        )

    @BeforeEach
    fun setupSecurityContext() {
        val idToken =
            OidcIdToken.withTokenValue("token")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject("test-user")
                .claim("name", "Test User")
                .claim("email", "test@example.com")
                .build()
        val oidcUser =
            DefaultOidcUser(
                listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
                idToken,
            )
        val authentication = UsernamePasswordAuthenticationToken(oidcUser, null, oidcUser.authorities)
        SecurityContextHolder.getContext().authentication = authentication
    }

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `list without HX-Request returns decorated page`() {
        whenever(loggingEventService.search(any(), any())).thenReturn(emptyPage)

        val result = controller.list(LoggingFilter(), PageRequest.of(0, 10), isHxRequest = false)

        assertThat(result.viewName).isEqualTo("$BASE_FRAGMENT_LOGGING/list-page-logging")
        assertThat(result.model).containsKey("events")
        assertThat(result.model).containsKey("page")
        assertThat(result.model).containsKey("filter")
    }

    @Test
    fun `list with HX-Request returns list fragment`() {
        whenever(loggingEventService.search(any(), any())).thenReturn(emptyPage)

        val result = controller.list(LoggingFilter(), PageRequest.of(0, 10), isHxRequest = true)

        assertThat(result.viewName).isEqualTo("$BASE_FRAGMENT_LOGGING/list")
        assertThat(result.model).containsKey("events")
    }

    @Test
    fun `filter with HX-Request returns results and pagination views`() {
        whenever(loggingEventService.search(any(), any())).thenReturn(emptyPage)

        val views = controller.filter(LoggingFilter(), PageRequest.of(0, 10), isHxRequest = true)

        assertThat(views).hasSize(2)
        assertThat(views[0].viewName).isEqualTo("$BASE_FRAGMENT_LOGGING/filter-results")
        assertThat(views[1].viewName).isEqualTo("$BASE_FRAGMENT_LOGGING/pagination")
    }

    @Test
    fun `filter without HX-Request returns full page`() {
        whenever(loggingEventService.search(any(), any())).thenReturn(emptyPage)

        val views = controller.filter(LoggingFilter(), PageRequest.of(0, 10), isHxRequest = false)

        assertThat(views).hasSize(1)
        assertThat(views[0].viewName).isEqualTo("$BASE_FRAGMENT_LOGGING/list-page-logging")
    }

    @Test
    fun `filter passes message param to service`() {
        whenever(loggingEventService.search(any(), any())).thenReturn(emptyPage)
        val filter = LoggingFilter(message = "test-message")

        controller.filter(filter, PageRequest.of(0, 10), isHxRequest = true)

        verify(loggingEventService).search(
            argThat { message == "test-message" },
            any(),
        )
    }

    @Test
    fun `filter passes level param to service`() {
        whenever(loggingEventService.search(any(), any())).thenReturn(emptyPage)
        val filter = LoggingFilter(level = "WARN")

        controller.filter(filter, PageRequest.of(0, 10), isHxRequest = true)

        verify(loggingEventService).search(
            argThat { level == "WARN" },
            any(),
        )
    }
}