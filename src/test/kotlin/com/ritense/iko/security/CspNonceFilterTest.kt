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
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class CspNonceFilterTest {
    private val filter = CspNonceFilter("script-src {nonce} 'strict-dynamic'")

    @Test
    fun `writes CSP header with a nonce and sets request attribute`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, MockFilterChain())

        val header = response.getHeader("Content-Security-Policy")!!
        assertThat(header).matches("script-src 'nonce-[A-Za-z0-9+/=]+' 'strict-dynamic'")
        assertThat(request.getAttribute("cspNonce")).isEqualTo(
            header.substringAfter("'nonce-").substringBefore("'"),
        )
    }

    @Test
    fun `generates a fresh nonce per request`() {
        fun nonceOf(): String {
            val res = MockHttpServletResponse()
            filter.doFilter(MockHttpServletRequest(), res, MockFilterChain())
            return res.getHeader("Content-Security-Policy")!!
        }
        assertThat(nonceOf()).isNotEqualTo(nonceOf())
    }

    @Test
    fun `admin CSP template does not contain unsafe-eval`() {
        assertThat(ADMIN_CSP_TEMPLATE).doesNotContain("'unsafe-eval'")
    }
}