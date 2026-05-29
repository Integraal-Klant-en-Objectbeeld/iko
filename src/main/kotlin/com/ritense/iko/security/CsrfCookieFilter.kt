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
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Forces resolution of the deferred CSRF token so the `XSRF-TOKEN` cookie is
 * materialised on every response. Spring Security 6's default is BREACH-resistant
 * and writes the cookie only when the token is read by a request handler, which
 * never happens for pure HTMX flows that read the cookie from JavaScript.
 */
internal class CsrfCookieFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val csrfToken = request.getAttribute(CsrfToken::class.java.name) as? CsrfToken
        csrfToken?.token // Enforce getting the token on each request, so token is refreshed
        filterChain.doFilter(request, response)
    }
}