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

import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Component

/**
 * Per-session admin UI timeout, in seconds. The countdown shown by the
 * session-timeout modal is derived from these values.
 */
internal data class SessionTimeout(
    val timeoutSeconds: Long,
    val warningSeconds: Long,
)

/**
 * Resolves the admin UI session timeout by querying the servlet [HttpSession]
 * directly, rather than re-parsing the Keycloak refresh token on every page
 * render.
 *
 * The session's `maxInactiveInterval` is the authoritative "time until the
 * server ends the session for inactivity". It is initialised from
 * `server.servlet.session.timeout` at login and re-seeded from the refreshed
 * Keycloak `refresh_expires_in` on every keep-alive ping (see
 * [com.ritense.iko.mvc.controller.SessionController]), so reading it here yields
 * the same token-driven timeout without any token/JWT plumbing.
 *
 * The warning window is derived as `min(configured warning-before, timeout / 2)`
 * so the JS `timeoutSec > warningSec` guard always holds. When no session is
 * available (or its interval is non-positive, i.e. "never expires") this falls
 * back to the static [AdminSessionProperties] defaults so the modal is always
 * driven by sane values.
 */
@Component
internal class AdminSessionTimeoutResolver(
    private val adminSessionProperties: AdminSessionProperties,
) {
    fun resolve(session: HttpSession?): SessionTimeout {
        val timeoutSeconds = session?.maxInactiveInterval
            ?.toLong()
            ?.takeIf { it > 0 }
            ?: adminSessionProperties.timeoutSeconds

        val warningSeconds = minOf(adminSessionProperties.warningBeforeSeconds, timeoutSeconds / 2)

        return SessionTimeout(timeoutSeconds, warningSeconds)
    }
}