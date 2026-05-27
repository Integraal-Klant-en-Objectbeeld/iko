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

import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.AuthenticationException

class HtmxAuthenticationEntryPointTest {
    private val entryPoint = HtmxAuthenticationEntryPoint()

    @Test
    fun `commence responds with 401 and HX-Redirect header`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        entryPoint.commence(request, response, mock<AuthenticationException>())

        assertThat(response.status).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED)
        assertThat(response.getHeader("HX-Redirect")).isEqualTo("/admin")
    }
}