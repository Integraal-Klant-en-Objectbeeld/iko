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

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import java.security.SecureRandom
import java.util.Base64

internal class CspNonceFilter(
    private val policyTemplate: String,
    private val headerName: String = "Content-Security-Policy",
) : OncePerRequestFilter() {
    private val secureRandom = SecureRandom()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val nonce = Base64.getEncoder().encodeToString(ByteArray(32).also(secureRandom::nextBytes))
        request.setAttribute(NONCE_ATTRIBUTE, nonce)
        response.setHeader(headerName, policyTemplate.replace(NONCE_PLACEHOLDER, "'nonce-$nonce'"))
        filterChain.doFilter(request, response)
    }

    companion object {
        const val NONCE_ATTRIBUTE = "cspNonce"
        const val NONCE_PLACEHOLDER = "{nonce}"
    }
}