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

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint

/**
 * Authentication entry point for HTMX requests on the admin UI. Instead of the
 * default 302 redirect to the OAuth2 authorization endpoint (which HTMX would
 * follow via XHR and inject the login page into the content panel), this
 * responds with a 401 and an `HX-Redirect` header. HTMX honours that header by
 * performing a full client-side navigation to `/admin`, which then triggers the
 * normal browser login flow.
 */
internal class HtmxAuthenticationEntryPoint : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.setHeader("HX-Redirect", "/admin")
        response.status = HttpServletResponse.SC_UNAUTHORIZED
    }
}